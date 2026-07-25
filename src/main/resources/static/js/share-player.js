document.addEventListener("DOMContentLoaded", () => {
    const storageKey = "agtydrive_video_volume";
    const players = document.querySelectorAll("[data-video-player]");

    const formatTime = (seconds) => {
        if (!Number.isFinite(seconds) || seconds < 0) {
            return "00:00";
        }

        const total = Math.floor(seconds);
        const hours = Math.floor(total / 3600);
        const minutes = Math.floor((total % 3600) / 60);
        const secs = total % 60;

        if (hours > 0) {
            return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
        }

        return `${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
    };

    const readStoredVolume = () => {
        const rawValue = window.localStorage.getItem(storageKey);
        const parsedValue = rawValue == null ? Number.NaN : Number.parseFloat(rawValue);

        if (!Number.isFinite(parsedValue)) {
            return 0.5;
        }

        return Math.min(1, Math.max(0, parsedValue));
    };

    const updateTime = (video, timeElement) => {
        timeElement.textContent = `${formatTime(video.currentTime)} / ${formatTime(video.duration)}`;
    };

    const updateToggle = (video, toggleButton) => {
        toggleButton.classList.remove("is-play", "is-pause");
        if (video.paused) {
            toggleButton.classList.add("is-play");
            toggleButton.setAttribute("aria-label", "Воспроизвести");
        } else {
            toggleButton.classList.add("is-pause");
            toggleButton.setAttribute("aria-label", "Пауза");
        }
    };

    const updateMuteButton = (video, muteButton) => {
        if (!muteButton) {
            return;
        }

        const muted = video.muted || video.volume === 0;
        muteButton.classList.toggle("is-muted", muted);
        muteButton.setAttribute("aria-label", muted ? "Включить звук" : "Выключить звук");
    };

    players.forEach((player) => {
        const media = player.querySelector("[data-video-element]");
        const mediaStage = player.querySelector("[data-media-stage]");
        const toggleButton = player.querySelector("[data-video-toggle]");
        const progressInput = player.querySelector("[data-video-progress]");
        const volumeInput = player.querySelector("[data-video-volume]");
        const timeElement = player.querySelector("[data-video-time]");
        const muteButton = player.querySelector("[data-video-mute]");

        if (!media || !toggleButton || !progressInput || !volumeInput || !timeElement) {
            return;
        }

        let isSeeking = false;

        const applyVolume = (value) => {
            media.volume = value;
            media.muted = value === 0;
            volumeInput.value = String(Math.round(value * 100));
            updateMuteButton(media, muteButton);
        };

        const seekToPercent = () => {
            if (!Number.isFinite(media.duration) || media.duration <= 0) {
                return;
            }

            const percent = Number.parseFloat(progressInput.value);
            if (!Number.isFinite(percent)) {
                return;
            }

            media.currentTime = (percent / 100) * media.duration;
            updateTime(media, timeElement);
        };

        const seekToClientX = (clientX) => {
            if (!Number.isFinite(media.duration) || media.duration <= 0) {
                return;
            }

            const rect = progressInput.getBoundingClientRect();
            if (!rect.width) {
                return;
            }

            const offsetX = Math.min(rect.width, Math.max(0, clientX - rect.left));
            const percent = (offsetX / rect.width) * 100;
            progressInput.value = String(percent);
            media.currentTime = (percent / 100) * media.duration;
            updateTime(media, timeElement);
        };

        applyVolume(readStoredVolume());
        updateTime(media, timeElement);
        updateToggle(media, toggleButton);
        player.classList.toggle("is-playing", !media.paused);

        const togglePlayback = () => {
            if (media.paused) {
                void media.play();
            } else {
                media.pause();
            }
        };

        toggleButton.addEventListener("click", togglePlayback);
        if (media.tagName === "VIDEO") {
            media.addEventListener("click", togglePlayback);
        } else if (mediaStage) {
            mediaStage.addEventListener("click", togglePlayback);
        }

        media.addEventListener("play", () => {
            updateToggle(media, toggleButton);
            player.classList.add("is-playing");
        });

        media.addEventListener("pause", () => {
            updateToggle(media, toggleButton);
            player.classList.remove("is-playing");
        });

        media.addEventListener("loadedmetadata", () => updateTime(media, timeElement));
        media.addEventListener("timeupdate", () => {
            if (!isSeeking && Number.isFinite(media.duration) && media.duration > 0) {
                progressInput.value = String((media.currentTime / media.duration) * 100);
            } else if (!isSeeking) {
                progressInput.value = "0";
            }

            updateTime(media, timeElement);
        });

        progressInput.addEventListener("pointerdown", () => {
            isSeeking = true;
        });

        progressInput.addEventListener("input", seekToPercent);

        progressInput.addEventListener("click", (event) => {
            seekToClientX(event.clientX);
        });

        progressInput.addEventListener("change", () => {
            seekToPercent();
            isSeeking = false;
        });

        progressInput.addEventListener("pointerup", (event) => {
            seekToClientX(event.clientX);
            isSeeking = false;
        });

        progressInput.addEventListener("blur", () => {
            isSeeking = false;
        });

        volumeInput.addEventListener("input", () => {
            const percent = Number.parseFloat(volumeInput.value);
            if (!Number.isFinite(percent)) {
                return;
            }

            const normalized = Math.min(1, Math.max(0, percent / 100));
            applyVolume(normalized);
            window.localStorage.setItem(storageKey, String(normalized));
        });

        if (muteButton) {
            muteButton.addEventListener("click", () => {
                if (media.muted || media.volume === 0) {
                    const restoredValue = readStoredVolume();
                    applyVolume(restoredValue > 0 ? restoredValue : 0.5);
                } else {
                    applyVolume(0);
                }
            });
        }
    });
});

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
        const windowButton = player.querySelector("[data-video-window]");
        const fullscreenButton = player.querySelector("[data-video-fullscreen]");
        const windowIcon = player.querySelector("[data-video-window-icon]");
        const fullscreenIcon = player.querySelector("[data-video-fullscreen-icon]");
        const isVideoPlayer = media.tagName === "VIDEO";

        if (!media || !toggleButton || !progressInput || !volumeInput || !timeElement) {
            return;
        }

        let isSeeking = false;
        let controlsHideTimer = null;

        const isFullscreenActive = () => document.fullscreenElement === player;

        const clearControlsHideTimer = () => {
            if (controlsHideTimer !== null) {
                window.clearTimeout(controlsHideTimer);
                controlsHideTimer = null;
            }
        };

        const hideOverlayControls = () => {
            if (!isFullscreenActive()) {
                return;
            }
            player.classList.remove("show-overlay-controls");
        };

        const scheduleControlsHide = () => {
            clearControlsHideTimer();
            if (!isFullscreenActive()) {
                return;
            }
            controlsHideTimer = window.setTimeout(() => {
                hideOverlayControls();
            }, 2200);
        };

        const revealOverlayControls = () => {
            if (!isFullscreenActive()) {
                return;
            }
            player.classList.add("show-overlay-controls");
            scheduleControlsHide();
        };

        const updateWindowButton = () => {
            if (!windowButton) {
                return;
            }

            const isWindowMode = player.classList.contains("is-window-mode");
            windowButton.classList.toggle("is-active", isWindowMode);
            const label = isWindowMode ? "Вернуть обычный размер" : "Развернуть по ширине окна";
            windowButton.setAttribute("aria-label", label);
            windowButton.setAttribute("title", label);
            windowButton.setAttribute("data-tooltip", label);
            if (windowIcon) {
                windowIcon.src = isWindowMode ? "/icons/video-window-exit.svg" : "/icons/video-window-enter.svg";
            }
        };

        const updateFullscreenButton = () => {
            if (!fullscreenButton) {
                return;
            }

            const isFullscreen = document.fullscreenElement === player;
            fullscreenButton.classList.toggle("is-active", isFullscreen);
            const label = isFullscreen ? "Выйти из полноэкранного режима" : "Развернуть на весь экран";
            fullscreenButton.setAttribute("aria-label", label);
            fullscreenButton.setAttribute("title", label);
            fullscreenButton.setAttribute("data-tooltip", label);
            if (fullscreenIcon) {
                fullscreenIcon.src = isFullscreen ? "/icons/video-fullscreen-exit.svg" : "/icons/video-fullscreen-enter.svg";
            }
            player.classList.toggle("is-fullscreen-active", isFullscreen);
            player.classList.toggle("show-overlay-controls", isFullscreen);
            if (isFullscreen) {
                fullscreenButton.blur();
                scheduleControlsHide();
            } else {
                clearControlsHideTimer();
            }
        };

        const exitWindowMode = () => {
            player.classList.remove("is-window-mode");
            updateWindowButton();
        };

        const toggleWindowMode = () => {
            const activeWindowPlayer = document.querySelector(".share-video-player.is-window-mode");
            if (activeWindowPlayer && activeWindowPlayer !== player) {
                activeWindowPlayer.classList.remove("is-window-mode");
                const activeButton = activeWindowPlayer.querySelector("[data-video-window]");
                if (activeButton) {
                    activeButton.classList.remove("is-active");
                    activeButton.setAttribute("aria-label", "Развернуть по ширине окна");
                    activeButton.setAttribute("title", "Развернуть по ширине окна");
                    activeButton.setAttribute("data-tooltip", "Развернуть по ширине окна");
                    const activeIcon = activeWindowPlayer.querySelector("[data-video-window-icon]");
                    if (activeIcon) {
                        activeIcon.src = "/icons/video-window-enter.svg";
                    }
                }
            }

            player.classList.toggle("is-window-mode");
            updateWindowButton();
        };

        const toggleFullscreen = async () => {
            if (!document.fullscreenEnabled) {
                return;
            }

            if (document.fullscreenElement === player) {
                await document.exitFullscreen();
                return;
            }

            if (document.fullscreenElement) {
                await document.exitFullscreen();
            }

            await player.requestFullscreen();
        };

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
        updateWindowButton();
        updateFullscreenButton();
        player.classList.toggle("is-playing", !media.paused);

        const togglePlayback = () => {
            revealOverlayControls();
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
            scheduleControlsHide();
        });

        media.addEventListener("pause", () => {
            updateToggle(media, toggleButton);
            player.classList.remove("is-playing");
            revealOverlayControls();
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
            revealOverlayControls();
        });

        progressInput.addEventListener("input", seekToPercent);

        progressInput.addEventListener("click", (event) => {
            seekToClientX(event.clientX);
        });

        progressInput.addEventListener("change", () => {
            seekToPercent();
            isSeeking = false;
            scheduleControlsHide();
        });

        progressInput.addEventListener("pointerup", (event) => {
            seekToClientX(event.clientX);
            isSeeking = false;
            scheduleControlsHide();
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
            revealOverlayControls();
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

        if (isVideoPlayer && windowButton) {
            windowButton.addEventListener("click", toggleWindowMode);
        }

        if (isVideoPlayer && fullscreenButton) {
            fullscreenButton.addEventListener("click", () => {
                void toggleFullscreen();
            });
            document.addEventListener("fullscreenchange", updateFullscreenButton);
        }

        if (isVideoPlayer) {
            player.addEventListener("mousemove", revealOverlayControls);
            player.addEventListener("mouseenter", revealOverlayControls);
            player.addEventListener("mouseleave", () => {
                if (isFullscreenActive()) {
                    hideOverlayControls();
                    clearControlsHideTimer();
                }
            });
            player.addEventListener("focusin", revealOverlayControls);
            player.addEventListener("touchstart", revealOverlayControls, {passive: true});
            player.addEventListener("keydown", revealOverlayControls);
        }

        media.addEventListener("ended", exitWindowMode);
    });
});

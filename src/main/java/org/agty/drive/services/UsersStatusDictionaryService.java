package org.agty.drive.services;

import org.agty.drive.dto.UsersStatusDictionaryDto;
import org.agty.drive.repository.UsersStatusDictionaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersStatusDictionaryService {

    private final UsersStatusDictionaryRepository usersStatusDictionaryRepository;

    public UsersStatusDictionaryService(UsersStatusDictionaryRepository usersStatusDictionaryRepository) {
        this.usersStatusDictionaryRepository = usersStatusDictionaryRepository;
    }

    public List<UsersStatusDictionaryDto> findAll() {
        return usersStatusDictionaryRepository.findAll();
    }

    public UsersStatusDictionaryDto save(UsersStatusDictionaryDto dto) {
        return usersStatusDictionaryRepository.save(dto);
    }

    public UsersStatusDictionaryDto findById(Long id) {
        return usersStatusDictionaryRepository.findById(id);
    }
}

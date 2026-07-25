package org.agty.drive.services;

import org.agty.drive.dto.UsersRoleDictionaryDto;
import org.agty.drive.repository.UsersRoleDictionaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersRoleDictionaryService {

    private final UsersRoleDictionaryRepository usersRoleDictionaryRepository;

    public UsersRoleDictionaryService(UsersRoleDictionaryRepository usersRoleDictionaryRepository) {
        this.usersRoleDictionaryRepository = usersRoleDictionaryRepository;
    }

    public List<UsersRoleDictionaryDto> findAll() {
        return usersRoleDictionaryRepository.findAll();
    }

    public UsersRoleDictionaryDto save(UsersRoleDictionaryDto dto) {
        return usersRoleDictionaryRepository.save(dto);
    }

    public UsersRoleDictionaryDto findById(Long id) {
        return usersRoleDictionaryRepository.findById(id);
    }
}

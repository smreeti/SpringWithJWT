package com.smriti.service;

import com.smriti.constants.CacheNameConstants;
import com.smriti.entities.User;
import com.smriti.exceptionHandler.NoContentFoundException;
import com.smriti.repository.UserRepository;
import com.smriti.requestDTO.UserDTO;
import com.smriti.utility.MapperUtility;
import com.smriti.utility.UserQueryCreator;
import com.smriti.utility.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional("transactionManager")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void saveUser(UserDTO userDTO) {

        if (!Objects.isNull(cacheManager.getCache(CacheNameConstants.CACHE_NAME))) {
            cacheManager.getCache(CacheNameConstants.CACHE_NAME).clear();
        }

        User user = MapperUtility.map(userDTO, User.class);
        user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
        userRepository.save(user);
    }

    @Cacheable(value = CacheNameConstants.CACHE_NAME)
    //enables Spring Caching functionality
    @Override
    public List<UserDTO> fetchAllUsers() {
        Query query = entityManager.createNativeQuery
                (UserQueryCreator.createQueryToFetchAllUsers());
        List<Object[]> results = query.getResultList();

        validateResultSize.accept(results);

        return results.stream().map(UserUtils.convertToUserResponse)
                .collect(Collectors.toList());
    }

    public User getUserByUsername(String username) {
        User user =  userRepository.getUserByUsername(username);

        if (Objects.isNull(user))
            throw new UsernameNotFoundException(String.format("No user found with username '%'.", username));
        return user;
    }

    public Consumer<List<Object[]>> validateResultSize = (results)->{
        if (results.size() < 0)
            throw new NoContentFoundException("No records found", "No records found");
    };
}

package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findAll_returnsAllUsers() {
        User u = new User(1, "Alice", "alice@example.com", "secret", "123", User.Role.customer, null);
        when(userRepository.findAll()).thenReturn(List.of(u));

        List<User> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findById_returnsUser_whenPresent() {
        User u = new User(1, "Alice", "alice@example.com", "secret", "123", User.Role.customer, null);
        when(userRepository.findById(1)).thenReturn(Optional.of(u));

        Optional<User> result = userService.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Alice");
    }

    @Test
    void findById_returnsEmpty_whenAbsent() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(userService.findById(99)).isEmpty();
    }

    @Test
    void save_persists() {
        User u = new User(null, "Bob", "bob@example.com", "pass", "456", User.Role.customer, null);
        User saved = new User(2, "Bob", "bob@example.com", "pass", "456", User.Role.customer, null);
        when(userRepository.save(u)).thenReturn(saved);

        User result = userService.save(u);

        assertThat(result.getId()).isEqualTo(2);
    }

    @Test
    void deleteById_delegates() {
        doNothing().when(userRepository).deleteById(1);
        userService.deleteById(1);
        verify(userRepository).deleteById(1);
    }
}

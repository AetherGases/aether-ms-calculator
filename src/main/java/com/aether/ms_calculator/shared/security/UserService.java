package com.aether.ms_calculator.shared.security;

import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import com.aether.ms_calculator.shared.helpers.CustomUserDetails;
import com.aether.ms_calculator.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

  private final EmployeeRepository employeeRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String email) {
    EmployeeEntity employee =  employeeRepository.findByEmail(email).orElseThrow(
        () -> new UnauthorizedException("exception.login.invalid")
    );

    List<SimpleGrantedAuthority> authorities = employee.getPermissionGroup()
        .getPermissions()
        .stream()
        .distinct()
        .map(
            permission -> new SimpleGrantedAuthority(permission.getName())
        ).toList();

    return new CustomUserDetails(
        employee.getEmail(),
        employee.getPasswordHash(),
        authorities,
        employee.getId()
    );
  }
}

package com.smartflow.service;

import com.smartflow.dto.ClientDtos;
import com.smartflow.entity.Client;
import com.smartflow.entity.Role;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.UserMapper;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des clients.
 */
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InterventionRepository interventionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<ClientDtos.ClientResponse> listAll() {
        return clientRepository.findAllByOrderByCompanyNameAsc().stream()
                .map(UserMapper::toClientResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientDtos.ClientResponse get(Long id) {
        return UserMapper.toClientResponse(find(id));
    }

    @Transactional
    public ClientDtos.ClientResponse create(ClientDtos.CreateClientRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        Client client = Client.builder()
                .user(user)
                .companyName(request.companyName())
                .address(request.address())
                .city(request.city())
                .siret(request.siret())
                .build();
        return UserMapper.toClientResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientDtos.ClientResponse update(Long id, ClientDtos.UpdateClientRequest request) {
        Client client = find(id);
        if (request.phone() != null) {
            client.getUser().setPhone(request.phone());
        }
        if (request.companyName() != null) {
            client.setCompanyName(request.companyName());
        }
        if (request.address() != null) {
            client.setAddress(request.address());
        }
        if (request.city() != null) {
            client.setCity(request.city());
        }
        if (request.siret() != null) {
            client.setSiret(request.siret());
        }
        return UserMapper.toClientResponse(client);
    }

    @Transactional
    public void delete(Long id) {
        Client client = find(id);
        if (interventionRepository.countByClientId(id) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Impossible de supprimer le client : il possède des interventions");
        }
        userRepository.delete(client.getUser());
        clientRepository.delete(client);
    }

    @Transactional(readOnly = true)
    public Client findClientByUserId(Long userId) {
        return clientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client (userId)", userId));
    }

    public Client find(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }
}
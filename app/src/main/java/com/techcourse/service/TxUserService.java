package com.techcourse.service;

import com.techcourse.domain.User;
import org.springframework.transaction.support.TransactionTemplate;

public class TxUserService implements UserService {

    private final AppUserService appUserService;
    private final TransactionTemplate transactionTemplate;

    public TxUserService(final AppUserService appUserService, final TransactionTemplate transactionTemplate) {
        this.appUserService = appUserService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public User findById(final long id) {
        return appUserService.findById(id);
    }

    @Override
    public void insert(final User user) {
        transactionTemplate.execute(connection -> appUserService.insert(user));
    }

    @Override
    public void changePassword(final long id, final String newPassword, final String createBy) {
        transactionTemplate.execute(connection -> appUserService.changePassword(id, newPassword, createBy));
    }
}

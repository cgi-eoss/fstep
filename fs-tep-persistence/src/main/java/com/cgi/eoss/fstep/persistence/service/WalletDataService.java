package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.model.WalletTransaction;

public interface WalletDataService extends
        FstepEntityDataService<Wallet> {
    Wallet findByOwner(User user);
    void transact(WalletTransaction transaction);
    void creditBalance(Wallet wallet, int amount);
}

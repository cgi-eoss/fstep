package com.cgi.eoss.fstep.persistence.dao;

import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.Wallet;

public interface WalletDao extends FstepEntityDao<Wallet> {
    Wallet findOneByOwner(User user);
}

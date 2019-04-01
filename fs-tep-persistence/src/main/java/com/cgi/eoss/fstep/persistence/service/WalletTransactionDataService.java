package com.cgi.eoss.fstep.persistence.service;

import java.util.List;

import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction.Type;

public interface WalletTransactionDataService extends FstepEntityDataService<WalletTransaction> {
	
	public List<WalletTransaction> findByTypeAndAssociatedId(Type type, Long associatedId);
	
}

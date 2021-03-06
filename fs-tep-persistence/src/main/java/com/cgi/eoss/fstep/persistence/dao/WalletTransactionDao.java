package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;

import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction.Type;

public interface WalletTransactionDao extends FstepEntityDao<WalletTransaction> {
	
	public List<WalletTransaction> findByTypeAndAssociatedId(Type type, Long associatedId);
	
}

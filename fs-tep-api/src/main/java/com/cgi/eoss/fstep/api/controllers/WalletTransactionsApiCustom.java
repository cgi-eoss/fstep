package com.cgi.eoss.fstep.api.controllers;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.WalletTransaction.Type;

public interface WalletTransactionsApiCustom {
	
	Page<WalletTransaction> parametricFind(User user, Collection<Type> type, Long associatedId, OffsetDateTime startDateTime,
			OffsetDateTime endDateTime, Pageable pageable);


}

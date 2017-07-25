package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.DownloaderCredentials;
import com.cgi.eoss.fstep.persistence.dao.DownloaderCredentialsDao;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.fstep.model.QDownloaderCredentials.downloaderCredentials;

@Service
@Transactional(readOnly = true)
public class JpaDownloaderCredentialsDataService extends AbstractJpaDataService<DownloaderCredentials> implements DownloaderCredentialsDataService {

    private final DownloaderCredentialsDao downloaderCredentialsDao;

    @Autowired
    public JpaDownloaderCredentialsDataService(DownloaderCredentialsDao downloaderCredentialsDao) {
        this.downloaderCredentialsDao = downloaderCredentialsDao;
    }

    @Override
    FstepEntityDao<DownloaderCredentials> getDao() {
        return downloaderCredentialsDao;
    }

    @Override
    Predicate getUniquePredicate(DownloaderCredentials entity) {
        return downloaderCredentials.host.eq(entity.getHost());
    }

    @Override
    public DownloaderCredentials getByHost(String host) {
        return downloaderCredentialsDao.findOne(downloaderCredentials.host.eq(host));
    }

}

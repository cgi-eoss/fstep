package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QFstepService.fstepService;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceDao;
import com.querydsl.core.types.Predicate;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<FstepService> implements ServiceDataService {

    private final FstepServiceDao fstepServiceDao;

    private ServiceFileDataService serviceFilesDataService;

    @Autowired
    public JpaServiceDataService(FstepServiceDao fstepServiceDao, ServiceFileDataService serviceFilesDataService) {
        this.fstepServiceDao = fstepServiceDao;
        this.serviceFilesDataService = serviceFilesDataService;
    }

    @Override
    FstepEntityDao<FstepService> getDao() {
        return fstepServiceDao;
    }

    @Override
    Predicate getUniquePredicate(FstepService entity) {
        return fstepService.name.eq(entity.getName());
    }

    @Override
    public List<FstepService> search(String term) {
        return fstepServiceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FstepService> findByOwner(User user) {
        return fstepServiceDao.findByOwner(user);
    }

    @Override
    public FstepService getByName(String serviceName) {
        return fstepServiceDao.findOne(fstepService.name.eq(serviceName));
    }

    @Override
    public List<FstepService> findAllAvailable() {
        return fstepServiceDao.findByStatus(FstepService.Status.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public String computeServiceFingerprint(FstepService fstepService) {
        ObjectOutputStream oos;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            oos = new ObjectOutputStream(bos);
            List<FstepServiceContextFile> serviceFiles = serviceFilesDataService.findByService(fstepService);
            for (FstepServiceContextFile contextFile : serviceFiles) {
                oos.writeObject(contextFile.getFilename());
                oos.writeObject(contextFile.getContent());
            }
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] serviceSerialized = bos.toByteArray();
            digest.update(serviceSerialized);
            String md5 = Hex.encodeHexString(digest.digest());
            return md5;

        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }

    }

}

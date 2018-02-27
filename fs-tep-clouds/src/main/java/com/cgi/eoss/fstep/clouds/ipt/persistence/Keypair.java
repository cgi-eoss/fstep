package com.cgi.eoss.fstep.clouds.ipt.persistence;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "keypairs")
@NoArgsConstructor
public class Keypair implements Serializable{
    @Id
    private String serverId;
    
    @Lob
    @Column(length = 10000)
    private String privateKey;
    
    @Lob 
    @Column(length = 1000)
    private String publicKey;

    public Keypair(String serverId, String privateKey, String publicKey) {
        super();
        this.serverId = serverId;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }
    
    
}
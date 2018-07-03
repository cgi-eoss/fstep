package com.cgi.eoss.fstep.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.cgi.eoss.fstep.model.FstepService.Type;
import com.cgi.eoss.fstep.model.converters.FstepServiceDescriptorYamlConverter;
import com.cgi.eoss.fstep.model.converters.FstepServiceResourcesYamlConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = {"id", "templateFiles"})
@ToString(exclude = {"serviceDescriptor", "templateFiles"})
@Table(name = "fstep_service_templates",
        indexes = {@Index(name = "fstep_service_templates_name_idx", columnList = "name"), @Index(name = "fstep_service_templates_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@NoArgsConstructor
@Entity
public class FstepServiceTemplate implements FstepEntityWithOwner<FstepServiceTemplate>, Searchable {

    /**
     * <p>Internal unique identifier of the service template.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Unique name of the service template, assigned by the owner.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * <p>Human-readable descriptive summary of the service.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The type of the template, e.g. 'processor' or 'application'.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type = Type.PROCESSOR;

    /**
     * <p>The user owning the template, typically the template creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The partial (template) definition of the WPS service.</p>
     */
    @Lob
    @Convert(converter = FstepServiceDescriptorYamlConverter.class)
    @Column(name = "wps_descriptor")
    private FstepServiceDescriptor serviceDescriptor;

    /**
     * <p>The files required to build this service's docker image.</p>
     */
    @OneToMany(mappedBy = "serviceTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<FstepServiceTemplateFile> templateFiles = new HashSet<>();

    @Lob
    @Convert(converter = FstepServiceResourcesYamlConverter.class)
    @Column(name = "required_resources")
    FstepServiceResources requiredResources;
    
    /**
     * <p>Create a new Template with the minimum required parameters.</p>
     *
     * @param name Name of the template.
     * @param owner The user owning the template.
     */
    public FstepServiceTemplate(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(FstepServiceTemplate o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

    public void setTemplateFiles(Set<FstepServiceTemplateFile> templateFiles) {
    	templateFiles.forEach(f -> f.setServiceTemplate(this));
        this.templateFiles = templateFiles;
    }
}

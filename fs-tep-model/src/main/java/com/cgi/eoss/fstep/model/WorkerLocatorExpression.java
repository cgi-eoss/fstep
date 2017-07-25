package com.cgi.eoss.fstep.model;

import com.google.common.collect.ComparisonChain;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_worker_locator_expressions",
        indexes = {@Index(name = "fstep_worker_locator_expressions_service_idx", columnList = "service")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"service"})})
@NoArgsConstructor
@Entity
public class WorkerLocatorExpression implements FstepEntity<WorkerLocatorExpression> {

    /**
     * <p>Unique internal identifier of the worker locator expression.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>The associated service for which this expression resolves a worker.</p>
     */
    @OneToOne
    @JoinColumn(name = "service", nullable = false)
    private FstepService service;

    /**
     * <p>Expression to be evaluated when resolving a worker for the associated service.</p>
     */
    @Column(name = "expression", nullable = false)
    private String expression;

    @Builder
    public WorkerLocatorExpression(FstepService service, String expression) {
        this.service = service;
        this.expression = expression;
    }

    @Override
    public int compareTo(WorkerLocatorExpression o) {
        return ComparisonChain.start().compare(id, o.id).result();
    }

}

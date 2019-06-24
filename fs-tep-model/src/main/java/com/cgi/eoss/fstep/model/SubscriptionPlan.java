package com.cgi.eoss.fstep.model;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.cgi.eoss.fstep.model.converters.CostQuotationYamlConverter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(of = {"name"})
@Table(name = "fstep_subscription_plans",
        indexes = {@Index(name = "fstep_subscription_plans_name_idx", columnList = "name")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_subscription_plans_name_idx", columnNames = {"name"})})
@NoArgsConstructor
@Entity
public class SubscriptionPlan implements FstepEntity<SubscriptionPlan>, Searchable {
    /**
     * <p>Internal unique identifier of the subscription plan.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * <p>Name of the subscription plan.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;
  
    /**
     * <p>Human-readable descriptive summary of the subscription plan.</p>
     */
    @Column(name = "description")
    private String description;
    
    /**
     * <p>The usage type the subscription plan refers to.</p>
     */
    @Column(name = "usage_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private UsageType usageType;
    
    /**
     * <p>The amount that constitutes a unit for this plan</p>
     */
    @Column(name = "unit", nullable = false)
    private long unit;
    
    /**
     * <p>The max amount of units that can be subscribed under this plan</p>
     */
    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;
    
    /**
     * <p>The max amount of units that can be subscribed under this plan</p>
     */
    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity;

    /**
     * <p>The usage type the quota refers to.</p>
     */
    @Column(name = "billing_scheme", nullable = false)
    @Enumerated(EnumType.STRING)
    private BillingScheme billingScheme;
    
    /**
     * <p>Cost of the subscription plan</p>
     */
    @Lob
    @Convert(converter = CostQuotationYamlConverter.class)
    @Column(name = "cost_quotation", nullable = false)
    private CostQuotation costQuotation;


    /**
     * <p>Create a new subscription plan with the minimum mandatory parameters.</p>
     *
     * @param name Name of the collection. Must be unique per user.
     * @param owner User owning the collection.
     */
    public SubscriptionPlan(String name,UsageType usageType, long unit, int minQuantity, int maxQuantity, BillingScheme billingScheme, CostQuotation costQuotation) {
        this.name = name;
        this.usageType = usageType;
        this.unit = unit;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.billingScheme = billingScheme;
        this.costQuotation = costQuotation;
    }

    @Override
    public int compareTo(SubscriptionPlan o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

}

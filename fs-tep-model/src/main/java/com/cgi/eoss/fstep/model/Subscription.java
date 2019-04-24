package com.cgi.eoss.fstep.model;

import java.time.OffsetDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "fstep_subscriptions",
        indexes = {@Index(name = "fstep_subscriptions_subscription_plan_idx", columnList = "subscription_plan"), @Index(name = "fstep_subscriptions_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(name = "fstep_subscriptions_subscription_plan_owner_created_idx", columnNames = {"subscription_plan", "owner", "created"})})
@NoArgsConstructor
@Entity
public class Subscription implements FstepEntityWithOwner<Subscription>, Searchable {
    /**
     * <p>Internal unique identifier of the subscription.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * <p>The subscriber.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p> Plan associated to this subscription</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscription_plan", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /**
     *<p> The quantity of the plan this subscription is subscribing</p>
     */
    @Column(name = "quantity", nullable = false)
    private int quantity;
    
    /**
     * /Time the subscription was created
     *
     */
    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    /**
     * End of subscription 
     *
     */
    @Column(name = "ended")
    private OffsetDateTime ended;
    
    /**
     * Start of the current subscription period
     *
     */
    @Column(name = "current_start")
    private OffsetDateTime currentStart;
    
    /**
     * End of the current subscription period
     *
     */
    @Column(name = "current_end")
    private OffsetDateTime currentEnd;
    
    /**
     * <p>Subscription status</p>
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;
    
    /**
     * <p> Quota this subscription generated</p>
     */
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "quota")
    private Quota quota;

    /**
     * <p> Should the subscription be renewed at the end of its period</p>
     */
    @Column(name = "renew")
    private boolean renew = true;
    
    /**
     * <p> Plan this subscription will switch to on downgrade</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "downgrade_plan")
    private SubscriptionPlan downgradePlan;

    /**
     *<p> The quantity of the plan this subscription will use when downgrade is applied</p>
     */
    @Column(name = "downgrade_quantity")
    private Integer downgradeQuantity;
    
    /**
     * <p>Create a new subscription with the minimum mandatory parameters.</p>
     * 
     * @param owner User owning the subscription.
     */
    public Subscription(User owner, SubscriptionPlan subscriptionPlan, int quantity, OffsetDateTime created) {
        this.owner = owner;
        this.subscriptionPlan = subscriptionPlan;
        this.quantity = quantity;
        this.created = created;
    }

    @Override
    public int compareTo(Subscription o) {
        return ComparisonChain.start().compare(owner.getId(), o.owner.getId()).compare(subscriptionPlan, o.getSubscriptionPlan()).result();
    }
    
    public enum Status {
    	CREATED, ACTIVE, TERMINATED, BLOCKED
    }

}

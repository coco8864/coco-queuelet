package naru.hibernate;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;


/** 
 *        @hibernate.class
 *         table="QUEUE_STORE"
 *     
*/
public class QueueStore implements Serializable {

    /** identifier field */
    private Integer id;

    /** nullable persistent field */
    private String treminal;

    /** nullable persistent field */
    private Object element;

    /** full constructor */
    public QueueStore(String treminal, Object element) {
        this.treminal = treminal;
        this.element = element;
    }

    /** default constructor */
    public QueueStore() {
    }

    /** 
     *            @hibernate.id
     *             generator-class="identity"
     *             type="java.lang.Integer"
     *             column="ID"
     *             unsaved-value="0"
     *         
     */
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /** 
     *            @hibernate.property
     *             column="TREMINAL"
     *             length="128"
     *         
     */
    public String getTreminal() {
        return this.treminal;
    }

    public void setTreminal(String treminal) {
        this.treminal = treminal;
    }

    /** 
     *            @hibernate.property
     *             column="ELEMENT"
     *         
     */
    public Object getElement() {
        return this.element;
    }

    public void setElement(Object element) {
        this.element = element;
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("id", getId())
            .toString();
    }

}

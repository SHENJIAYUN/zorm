package com.jia;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity  
@Inheritance(strategy=InheritanceType.JOINED)  
@Table(name="animals",catalog="orm" )
public class Animal {  
    private int id;  
    private String name;  
      
    @Id  
    @Column(name="id")
    public int getId() {  
        return id;  
    }  
    public void setId(int id) {  
        this.id = id;  
    }  
    
    @Column(name="name")
    public String getName() {  
        return name;  
    }  
    public void setName(String name) {  
        this.name = name;  
    }  
} 

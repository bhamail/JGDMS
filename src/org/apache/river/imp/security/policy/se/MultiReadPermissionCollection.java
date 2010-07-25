/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.river.imp.security.policy.se;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * MultiReadPermissionCollection is a wrapper class that enables mutliple
 * reads and RevokablePermissionCollection support.  It only supports
 * a homogenous class PermissionCollection.
 * 
 * TODO Serialization Correctly
 * @version 0.2 2009/11/14
 * @author Peter Firmstone
 */
public final class MultiReadPermissionCollection extends PermissionCollection 
    implements Serializable {
    private final static long serialVersionUID = 1L;
    private transient PermissionCollection permCl; // all access protected by rwl
    private final transient ReadWriteLock rwl;
    private final transient Lock rl;
    private final transient Lock wl;
    private boolean readOnly; // all access protected by rwl
    private Permission[] permissions; //never instantiate for ide code completion

    MultiReadPermissionCollection(Permission p){
        permCl = newPermissionCollection(p);
        rwl = new ReentrantReadWriteLock();
        rl = rwl.readLock();
        wl = rwl.writeLock();
        readOnly = false;
    }
    
    @Override
    public boolean isReadOnly(){
        rl.lock();
        try{  
            return readOnly;
        }finally {rl.unlock();}
    }
    
    @Override
    public void setReadOnly(){
        wl.lock();
        try{
            readOnly = true;
        }finally {wl.unlock();}
    }
    
    
    
    @Override
    public String toString(){
        rl.lock();
        try {
            return permCl.toString();
        } finally { rl.unlock();}
    }
    
    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if ( !(obj instanceof MultiReadPermissionCollection)) return false;
        MultiReadPermissionCollection mrpcObj = (MultiReadPermissionCollection) obj;
        rl.lock();
        try {
            if (permCl.equals(mrpcObj.permCl)) return true;
        }finally {rl.unlock();}
        return false;
    }
    
    public int hashCode(){
        rl.lock();
        try {
            return permCl.hashCode();
        }finally {rl.unlock();}
    }

    @Override
    public void add(Permission permission) {
        if (readOnly) {
            throw new SecurityException("attempt to add a Permission to a readonly Permissions object");
        } 
        wl.lock();
        try {
            permCl.add(permission);
        }
        finally {wl.unlock();}
    }

    @Override
    public boolean implies(Permission permission) {
        rl.lock();
        try {return permCl.implies(permission);}
        finally {rl.unlock();}
    }

    @Override
    public Enumeration<Permission> elements() {
        rl.lock();
        try {return permCl.elements();}
        finally {rl.unlock();}
    }
    
    
    /* Returns an empty PermissionCollection
     */ 
    private PermissionCollection newPermissionCollection(Permission permission){        
        PermissionCollection pc = permission.newPermissionCollection();
        if (pc == null){
            pc = new PermissionHash();
        }
        return pc;                    
    }   
    
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Permission[] permissions;
        private final boolean readOnly;
        SerializationProxy(PermissionCollection pc){
            ArrayList<Permission> collection = new ArrayList<Permission>();
            Enumeration<Permission> en = pc.elements();
            while (en.hasMoreElements()){
                collection.add(en.nextElement());
            }
            permissions = new Permission[collection.size()];
            collection.toArray(permissions);
            readOnly = pc.isReadOnly();
        }        
    }
    
    private Object writeReplace(){
        return new SerializationProxy(this);
    }
    
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
    
    private Object readResolve() {
        MultiReadPermissionCollection pc = new MultiReadPermissionCollection(permissions[0]);
        int length = permissions.length;
        for ( int i = 0 ; i < length ; i++){
            pc.add(permissions[i]);
        }
        if ( readOnly == true ) {pc.setReadOnly();}
        return pc;
    }
    
    private static final class PermissionHash extends PermissionCollection {
        // This class is never serialized.
        private final static long serialVersionUID = 1L;        
        private HashSet<Permission> permSet;

        public PermissionHash(){
            permSet = new HashSet<Permission>();
        }

        public boolean equals(Object obj){
            if (this == obj) return true;
            if (!(obj instanceof PermissionHash)) return false;
            PermissionHash phObj = (PermissionHash) obj;
            if (this.permSet.equals(phObj.permSet)) return true;
            return false;
        }

        public int hashCode(){
            return permSet.hashCode();
        }

        public String toString(){
            return permSet.toString();
        }

        @Override
        public void add(Permission permission) {            
            permSet.add(permission);            
        }

        @Override
        public boolean implies(Permission permission) {           
            // attempt a fast lookup and implies. If that fails
            // then enumerate through all the permissions.            
            if (permSet.contains(permission) 
                    && !(permission instanceof UnresolvedPermission)) {
                return true; 
            }
            Iterator<Permission> itr = permSet.iterator();
            while (itr.hasNext()) {
                Permission p = itr.next();
                if (p.implies(permission)) {return true;}
            }
            return false;            
        }

        @Override
        public Enumeration<Permission> elements() {
            synchronized (this){
                return Collections.enumeration(permSet);
            }
        }      
    }    
}
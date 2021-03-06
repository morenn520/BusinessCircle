package com.t.core.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.t.core.entities.NearByCategory;
import com.t.utils.BaseDao;

@Repository
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class NearByCategoryDao extends BaseDao<NearByCategory,Integer> {
	
	public NearByCategoryDao () {
		this.entityClass = NearByCategory.class;
	}
	
}

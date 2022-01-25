package com.nb.spring.product.model.service;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nb.spring.product.model.dao.ProductDao;
import com.nb.spring.product.model.vo.Product;
@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private SqlSessionTemplate session;
	
	@Autowired
	private ProductDao dao;
	
	@Override
	public Product selectOneProductNo(String productNo) {
		
		Product product = dao.selectOneProductNo(session, productNo);
		return product;
	}

}
/**
 * Copyright (C) 2013 Inera AB (http://www.inera.se)
 *
 * This file is part of Inera Axel (http://code.google.com/p/inera-axel).
 *
 * Inera Axel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Inera Axel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
/**
 * 
 */
package se.inera.axel.shs.broker.product.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.inera.axel.shs.broker.product.ProductAdminService;
import se.inera.axel.shs.broker.product.mongo.model.MongoShsProduct;
import se.inera.axel.shs.xml.product.ShsProduct;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Jan Hallonstén, R2M
 *
 */
@Service("productAdminService")
public class MongoProductAdminService extends MongoProductService implements ProductAdminService {

	@Override
	public void save(ShsProduct entity) {
		if (entity == null) {
			throw new IllegalArgumentException("Saved product must not be null");
		}
		MongoShsProduct product = assembler.assembleMongoShsProduct(entity);
		mongoShsProductRepository.save(product);
	}

	@Override
	public void delete(ShsProduct entity) {
		MongoShsProduct product = assembler.assembleMongoShsProduct(entity);
		mongoShsProductRepository.delete(product);
	}

	@Override
	public void delete(String productId) {
		mongoShsProductRepository.delete(productId);
	}

}

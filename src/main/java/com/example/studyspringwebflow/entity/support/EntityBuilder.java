package com.example.studyspringwebflow.entity.support;

import org.apache.commons.lang.ArrayUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;

public abstract class EntityBuilder<T extends Serializable> {

	protected T product;

	{
		initProduct();
	}

	public T build(Boolean... doNotPersist) {
		T product = assembleProduct();
		if (ArrayUtils.isEmpty(doNotPersist)
				|| (!ArrayUtils.isEmpty(doNotPersist) && doNotPersist[0] == Boolean.FALSE)) {
			EntityBuilderManager.getEntityManager().persist(product);
		}
		T temp = product;
		initProduct();
		return temp;
	}

	abstract void initProduct();

	abstract T assembleProduct();

	public static class EntityBuilderManager {
		private static ThreadLocal<EntityManager> entityManagerHolder = new ThreadLocal<EntityManager>();

		public static void setEntityManager(EntityManager entityManager) {
			entityManagerHolder.set(entityManager);
		}

		public static void clearEntityManager() {
			entityManagerHolder.remove();
		}

		public static EntityManager getEntityManager() {
			return entityManagerHolder.get();
		}
	}
}
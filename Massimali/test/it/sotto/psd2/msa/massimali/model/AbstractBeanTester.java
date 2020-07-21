package it.sotto.psd2.msa.massimali.model;

import akka.http.javadsl.testkit.JUnitRouteTest;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.meanbean.lang.Factory;
import org.meanbean.test.BeanTester;

public abstract class AbstractBeanTester<T> extends JUnitRouteTest {
 
    @Test
    public void testSerializable() throws Exception {
        T bean = getBeanInstance();
        byte[] serialized = SerializationUtils.serialize((Serializable)bean);
        @SuppressWarnings("unchecked")
        T deserialized = (T)SerializationUtils.deserialize(serialized);
        Assert.assertEquals(bean, deserialized);
    }
 
    @Test
    public void testGetterAndSetter() throws Exception {
        BeanTester beanTester = new BeanTester();
        beanTester.getFactoryCollection().addFactory(LocalDateTime.class, new LocalDateTimeFactory());
        beanTester.testBean(getBeanInstance().getClass());
    }

    @Test
    public void testNotEquals() throws Exception {
		Assert.assertNotEquals(getBeanInstance(), getDifferentBeanInstance());
		Assert.assertNotEquals(getBeanInstance(), null);
    }

    protected abstract T getBeanInstance();
	protected abstract T getDifferentBeanInstance();

	class LocalDateTimeFactory implements Factory<LocalDateTime> {
		@Override
		public LocalDateTime create() {
			return LocalDateTime.now();
		}
	}
}

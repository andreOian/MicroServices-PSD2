package it.sotto.psd2.msa.massimali.model;


import it.sotto.psd2.msa.Config;
import java.math.BigDecimal;

public class TestMassimaliModel extends AbstractBeanTester<MassimaliModel> {

	public TestMassimaliModel() {
		Config.loggerSetUp();
	}
	
	@Override
	protected MassimaliModel getBeanInstance() {
		MassimaliModel model = new MassimaliModel();
		model.setMaxGiornaliero(new BigDecimal(100));
		model.setMaxMensile(new BigDecimal(1000));
		model.setUtilizzatoGiornalieroOdierno(new BigDecimal(50));
		model.setUtilizzatoMensileCorrente(new BigDecimal(200));
		return model;
	}

	@Override
	protected MassimaliModel getDifferentBeanInstance() {
		MassimaliModel model = new MassimaliModel();
		model.setMaxGiornaliero(new BigDecimal(100));
		model.setMaxMensile(new BigDecimal(1000));
		model.setUtilizzatoGiornalieroOdierno(new BigDecimal(60));
		model.setUtilizzatoMensileCorrente(new BigDecimal(230));
		return model;
	}
}

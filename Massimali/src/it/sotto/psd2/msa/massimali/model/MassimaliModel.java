package it.sotto.psd2.msa.massimali.model;

import java.io.Serializable;
import java.math.BigDecimal;

@SuppressWarnings("serial")
public class MassimaliModel implements Serializable {
	private BigDecimal maxGiornaliero; //Massimo importo accordato per giorno
	private BigDecimal maxMensile; //Massimo importo accordato per mese
	private BigDecimal utilizzatoGiornalieroOdierno; //Importo già utilizzato nel giorno
	private BigDecimal utilizzatoMensileCorrente; //Importo già utilizzato nel mese
	
	public BigDecimal getMaxGiornaliero() {
		return maxGiornaliero;
	}
	public void setMaxGiornaliero(BigDecimal maxGiornaliero) {
		this.maxGiornaliero = maxGiornaliero;
	}
	
	public BigDecimal getMaxMensile() {
		return maxMensile;
	}
	public void setMaxMensile(BigDecimal maxMensile) {
		this.maxMensile = maxMensile;
	}
	
	public BigDecimal getUtilizzatoGiornalieroOdierno() {
		return utilizzatoGiornalieroOdierno;
	}
	public void setUtilizzatoGiornalieroOdierno(BigDecimal utilizzatoGiornalieroOdierno) {
		this.utilizzatoGiornalieroOdierno = utilizzatoGiornalieroOdierno;
	}
	
	public BigDecimal getUtilizzatoMensileCorrente() {
		return utilizzatoMensileCorrente;
	}
	public void setUtilizzatoMensileCorrente(BigDecimal utilizzatoMensileCorrente) {
		this.utilizzatoMensileCorrente = utilizzatoMensileCorrente;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof MassimaliModel) {
			MassimaliModel model=(MassimaliModel)obj;
			return (
				((maxGiornaliero!=null && maxGiornaliero.equals(model.maxGiornaliero))||(model.maxGiornaliero==null)) &&
				((maxMensile!=null && maxMensile.equals(model.maxMensile))||(model.maxMensile==null)) &&
				((utilizzatoGiornalieroOdierno!=null && utilizzatoGiornalieroOdierno.equals(model.utilizzatoGiornalieroOdierno))||(model.utilizzatoGiornalieroOdierno==null)) &&
				((utilizzatoMensileCorrente!=null && utilizzatoMensileCorrente.equals(model.utilizzatoMensileCorrente))||(model.utilizzatoMensileCorrente==null))
			);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
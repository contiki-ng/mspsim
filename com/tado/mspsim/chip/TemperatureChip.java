package com.tado.mspsim.chip;

/**
 * Interface for a temperature chip. 
 * 
 * Implementing this interface makes it compatible with the cooja-interfaces 
 * and hence makes the peripheral implementation way easier
 * 
 * @author Víctor Ariño <victor.arino@tado.com>
 */
public interface TemperatureChip {
	/**
	 * Get the current temperature on the chip
	 * 
	 * @return
	 */
	public int getTemperature();
	
	/**
	 * Set a new temperature on the chip
	 * 
	 * @param temp
	 * 		temperature to set in XXYY format where XX.YY°C
	 */
	public void setTemperature(int temp);
	
	/**
	 * Get the maximum temperature allowed by the chipset
	 * 
	 * @return
	 */
	public int getMaxTemperature();
	
	/**
	 * Get the minimum temperature allowed by the chipset
	 * 
	 * @return
	 */
	public int getMinTemperature();
}

package net.frontlinesms.messaging;

import java.util.HashMap;

import net.frontlinesms.resources.ResourceUtils;


public class CatHandlerAliasMatcher {
	/** Singleton instance of this class. */
	private static CatHandlerAliasMatcher instance;
	
	private HashMap<String, String> cathandlerAliases,
									manufacturerAliases,
									modelAliases;

	public void initAliases(String resourcesDirectory) {
		cathandlerAliases = initAliasesFromFile(resourcesDirectory + "conf/CATHandlerAliases.txt");
		manufacturerAliases = initAliasesFromFile(resourcesDirectory + "conf/manufacturerAliases.txt");
		modelAliases = initAliasesFromFile(resourcesDirectory + "conf/modelAliases.txt");
	}

	/** Create a new Cat Handler Alias Matcher. */
	private CatHandlerAliasMatcher() {
		super();
		
		this.initAliases (ResourceUtils.getConfigDirectoryPath());
	}
	
	/**
	 * @return The singleton instance of this class
	 */
	public static synchronized CatHandlerAliasMatcher getInstance() {
		if (instance == null) {
			instance = new CatHandlerAliasMatcher();
		}
		return instance;
	}
	
	
	/**
	 * Loads a translation map from a file of the following format: 
	 *	Split the line.  It should be of the following format:
	 *	<officialName><whiteSpace><alternateName1>,<alternateName2>,...,<alternateNameN>
	 * TODO this kind of thing should probably be done by the manager, or even at the UI layer.
	 * @param fileName name of the file to load the aliases from
	 * @return
	 */
	private final HashMap<String, String> initAliasesFromFile(String filename) {
		String[] fileContents = ResourceUtils.getUsefulLines(filename);
		
		// map from alternate names to offical names.
		HashMap<String, String> map = new HashMap<String, String>();
		for(String line : fileContents) {
			// Split the line.  It should be of the following format:
			// 	<officialName>		<alternateName1>,<alternateName2>,...,<alternateNameN>
			String[] words = line.split("\\s", 2);
			String officialName = words[0];
			map.put(officialName.toLowerCase(), officialName);
			if (words.length > 1) {
				words = words[1].split(",");
				for (String word : words) {
					map.put(word.trim().toLowerCase(), officialName);
				}
			}
		}
		
		return map;
	}
	
	/**
	 * Attempts to get a mapping from a particular make and model to a CATHandler
	 * 
	 * @param manufacturer
	 * @param model 
	 * @return
	 */
	public synchronized final String translateCATHandlerModel(String manufacturer, String model) {
		String lookupString = manufacturer.toLowerCase() + "_" + model.toLowerCase();
		String catHandler = cathandlerAliases.get(lookupString);
		return catHandler;
	}

	/**
	 * Translates the manufacture to a user-friendly string.
	 * 
	 * @param manufacturer
	 * @return
	 */
	public synchronized final String translateManufacturer(String manufacturer) {
		manufacturer = manufacturer.trim().toLowerCase();
		
		/** This fixes the issue caused by Huawei products, 
		 *  sometimes giving their name with strange characters inside 
		 *  during the detection */
		if (manufacturer.contains("huawei"))
			manufacturer = "huawei";
		
		String alias = manufacturerAliases.get(manufacturer);
		if (alias == null)
			return capitalise(manufacturer);
		else
			return alias;
	}

	/** Capitalise the first character of a string.
	 * Case of other letters is unchanged. */
	private String capitalise(String string) {
		if(string == null) return null;
		else if(string.length() <= 0) return "";
		else if(string.length() == 1) return string.toUpperCase();
		else {
			return string.substring(0, 1).toUpperCase() + string.substring(1);
		}
	}

	/**
	 * Translates the model to a user-friendly string.
	 * 
	 * @param model
	 * @return
	 */
	public synchronized final String translateModel(String manufacturer, String model) {
		model = model.trim();
		model = model.replace("\\s", "");
		model = model.replace(manufacturer, "");
		String alias = modelAliases.get(model.toLowerCase());
		if (alias == null) return model;
		else return alias;
	}
}

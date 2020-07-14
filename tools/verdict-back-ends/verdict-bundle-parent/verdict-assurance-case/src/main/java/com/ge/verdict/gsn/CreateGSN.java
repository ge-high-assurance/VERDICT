package com.ge.verdict.gsn;


import verdict.vdm.vdm_model.*;
import java.util.ArrayList;
import java.util.List;

public class CreateGSN {
	
	/**
	 * Entry method for the GSN class
	 * @author Saswata Paul
	 *
	 */
	public void execute(){
		
	}
	
	
	/**
	 * Produces a GSN fragment for each mission
	 * requirement a GSN and returns a list
	 * @param model
	 * @param node
	 * @return
	 */
	public List<GsnFragment> getFragments(Model model) {
		//List of gsn fragments
		List<GsnFragment> fragments = new ArrayList<>();
		
		for (Mission mission : model.getMission()) {
			fragments.add(populateFragment(mission, model));
		}
				
		return fragments;
	}
	
	
	/**
	 * Creates a GsnFragment with the given mission as rootnode
	 * @param mission
	 * @param model
	 * @return
	 */
	public GsnFragment populateFragment(Mission mission, Model model) {
		GsnFragment fragment = new GsnFragment();
		
		
		return fragment;
	}
	
}

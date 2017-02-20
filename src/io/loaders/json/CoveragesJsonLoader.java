package io.loaders.json;

import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecule;

import algorithms.utils.Coverage;
import algorithms.utils.Match;
import db.CoveragesDB;
import db.DB;
import db.FamilyDB;
import db.ResiduesDB;
import model.ChemicalObject;
import model.Monomer;
import model.Residue;
import model.graph.ContractedGraph;
import model.graph.MonomerGraph;
import model.graph.MonomerGraph.MonomerLinks;
import org.openscience.cdk.layout.StructureDiagramGenerator;

public class CoveragesJsonLoader extends
		AbstractJsonLoader<CoveragesDB, Coverage> {
	
	private DB<? extends ChemicalObject> db;
	private FamilyDB families;
	private ResiduesDB residues;

	public CoveragesJsonLoader(DB<? extends ChemicalObject> db, FamilyDB families) {
		this.db = db;
		this.families = families;
		this.residues = families.getResidues();
	}

	@Override
	protected CoveragesDB createDB() {
		return new CoveragesDB();
	}
	

	@Override
	protected Coverage objectFromJson(JSONObject obj) {
		// TODO : Create new parser with the files created by the new output functions
		System.err.println("From classe '" + CoveragesJsonLoader.class.getName() + "' method 'objectFromJson':");
		System.err.println("This is deprecated ! You will have some problems with recent json files !");
		ChemicalObject co = null;
		try {
			co = this.db.getObject("" + ((Number)obj.get("peptide")).intValue());
		} catch (NullPointerException e) {
			System.err.println(e.getMessage());
			System.err.println("Maybe coverage json and molecule json don't match");
			System.exit(2);
		}
		Coverage cov = new Coverage(co);
		
		JSONArray array = (JSONArray) obj.get("matches");
		for (Object o : array) {
			JSONObject jso = (JSONObject)o;
			Residue res = null;
			try {
				res = this.residues.getObject("" + ((Number)jso.get("residue")).intValue());
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			
			JSONObject jMatch = (JSONObject)jso.get("match");
			Match match = new Match(res);
				
			JSONArray atoms = (JSONArray) jMatch.get("atoms");
			for (Object atObj : atoms) {
				JSONObject atom = (JSONObject) atObj;
				int idx = ((Number)atom.get("a")).intValue();
				match.addAtom(idx);
				match.addHydrogens(idx, ((Number)atom.get("h")).intValue());
			}
			
			JSONArray bonds = (JSONArray) jMatch.get("bonds");
			for (Object boObj : bonds) {
				int bond = ((Number) boObj).intValue();
				match.addBond(bond);
			}
			
			cov.addMatch(match);
		}
		
		cov.calculateGreedyCoverage();
		
		return cov;
	}

	@Override
	protected String getObjectId(Coverage tObj) {
		return tObj.getId();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONArray getArrayOfElements(Coverage cov) {
		JSONArray array = new JSONArray();
		JSONObject obj = new JSONObject();
		
		obj.put("id", cov.getId());
		//obj.put("pepId", cov.getChemicalObject())
		obj.put("peptide", new Integer(cov.getChemicalObject().getId()));
		obj.put("peptideName", cov.getChemicalObject().getName());
		obj.put("atomic_graph", this.getJSONMatches(cov));
		obj.put("monomeric_graph", this.getJSONGraph(cov));
		obj.put("ratio", cov.getCoverageRatio());
		obj.put("correctness", cov.getCorrectness(families));

		array.add(obj);
		return array;
	}

	@SuppressWarnings("unchecked")
	private JSONObject getJSONMatches(Coverage cov) {
		JSONObject graph = new JSONObject();
		
		JSONArray atoms = new JSONArray();
		graph.put("atoms", atoms);
		JSONArray bonds = new JSONArray();
		graph.put("bonds", bonds);
		
		Set<IBond> usedBonds = new HashSet<>();

		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		IMolecule mol = cov.getChemicalObject().getMolecule();

		//calculate coordinates
		try {
			sdg.setMolecule(mol);
			sdg.generateCoordinates();
			mol = sdg.getMolecule();
		} catch (CDKException e) {
			//e.printStackTrace();
			throw new IllegalStateException("The coordiantes couldn't be calculated");
		}

		for (Match match : cov.getUsedMatches()) {
			// Atoms
			for (int a : match.getAtoms()) {
				JSONObject atom = new JSONObject();

				// CDK informations
				atom.put("cdk_idx", a);
				// Atom informations
				IAtom ia = mol.getAtom(a);
				//Coordinates informations
				JSONObject coordinates = new JSONObject();
				coordinates.put("x",ia.getPoint2d().x);
				coordinates.put("y",ia.getPoint2d().y);
				atom.put("coordiantes",coordinates);

				atom.put("name", ia.getSymbol());
				atom.put("hydrogens", match.getHydrogensFrom(a));
				// Residue informations
				atom.put("res", match.getResidue().getId());

				//ia.getPoint2d();

				atoms.add(atom);
			}
			
			// Bonds
			for (int b : match.getBonds()) {
				IBond ib = mol.getBond(b);
				usedBonds.add(ib);
				JSONObject bond = new JSONObject();
				
				// CDK informations
				bond.put("cdk_idx", b);
				
				// atoms linked
				JSONArray linkedAtoms = new JSONArray();
				for (IAtom a : ib.atoms()) {
					linkedAtoms.add(mol.getAtomNumber(a));
				}
				bond.put("arity", ib.getOrder().numeric());
				bond.put("atoms", linkedAtoms);
				bond.put("res", match.getResidue().getId());
				
				bonds.add(bond);
			}
		}
		

		for (IBond ib : mol.bonds()) {
			if (!usedBonds.contains(ib)) {
				JSONObject bond = new JSONObject();
				
				// CDK informations
				bond.put("cdk_idx", mol.getBondNumber(ib));
				
				// atoms linked
				JSONArray linkedAtoms = new JSONArray();
				for (IAtom a : ib.atoms()) {
					linkedAtoms.add(mol.getAtomNumber(a));
				}
				bond.put("arity", ib.getOrder().numeric());
				bond.put("atoms", linkedAtoms);
				
				bonds.add(bond);
			}
		}
		
		return graph;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject getJSONGraph(Coverage cov) {
		ContractedGraph cg = new ContractedGraph(cov);
		MonomerGraph mg = cg.toMonomerGraph(families);
		
		JSONObject graph = new JSONObject();
		// Monomers
		JSONArray monos = new JSONArray();
		for (Monomer mono : mg.nodes)
			if (mono != null)
				monos.add(mono.getName());
			else
				monos.add("?");
		graph.put("monos", monos);
		
		// Residues (equivalent to monomers)
		JSONArray residues = new JSONArray();
		for (Residue res : mg.residues)
			if (res != null)
				residues.add(res.getId());
			else
				residues.add("?");
		graph.put("residues", residues);
		
		// Links
		JSONArray links = new JSONArray();
		for (MonomerLinks ml : mg.links) {
			JSONObject link = new JSONObject();
			JSONArray idxs = new JSONArray();
			idxs.add(ml.mono1);
			idxs.add(ml.mono2);
			link.put("idxs", idxs);
			
			JSONArray type = new JSONArray();
			type.add(ml.label1);
			type.add(ml.label2);
			link.put("types", type);
			
			links.add(link);
		}
		graph.put("links", links);
		
		return graph;
	}

}

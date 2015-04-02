package mirroaccess;


public class Experiment {

	private String creator;
	private String name;
	private String description;
	private String address;
	private String filePath;
	private boolean isIndependent;
//	private Parcable image;
	
	public Experiment(String Creator, String Name, String Description, String address, String filePath, boolean isIndependent) {
		this.creator = Creator;
		this.name = Name;
		this.description = Description;
		this.address = address;
		this.filePath = filePath;
		this.isIndependent = isIndependent;
	}
	
	public String getCreator() {
		return this.creator;
	}
	public String getName() {
		return this.name;
	}
	public String getDescription() {
		return this.description;
	}
	public String getAddress() {
		return this.address;
	}
	public String getFilePath() {
		return this.filePath;
	}
	public boolean isIndependent() {
		return this.isIndependent;
	}
}

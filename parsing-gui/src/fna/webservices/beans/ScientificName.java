package fna.webservices.beans;

public class ScientificName {
	private String name;
	private String firstPublished;
	private String publicationYear;
	private String author;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the firstPublished
	 */
	public String getFirstPublished() {
		return firstPublished;
	}
	/**
	 * @param firstPublished the firstPublished to set
	 */
	public void setFirstPublished(String firstPublished) {
		this.firstPublished = firstPublished;
	}
	/**
	 * @return the publicationYear
	 */
	public String getPublicationYear() {
		return publicationYear;
	}
	/**
	 * @param publicationYear the publicationYear to set
	 */
	public void setPublicationYear(String publicationYear) {
		this.publicationYear = publicationYear;
	}
	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	public ScientificName (String name) {
		this.name = name;
	}
	
	public ScientificName () {}
	
	public ScientificName(String name, String firstPublished,
			String publicationYear, String author) {
		this.name = name;
		this.firstPublished = firstPublished;
		this.publicationYear = publicationYear;
		this.author = author;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((author == null) ? 0 : author.hashCode());
		result = prime * result
		+ ((firstPublished == null) ? 0 : firstPublished.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
		+ ((publicationYear == null) ? 0 : publicationYear.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ScientificName))
			return false;
		final ScientificName other = (ScientificName) obj;
		if (author == null) {
			if (other.author != null)
				return false;
		} else if (!author.equals(other.author))
			return false;
		if (firstPublished == null) {
			if (other.firstPublished != null)
				return false;
		} else if (!firstPublished.equals(other.firstPublished))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (publicationYear == null) {
			if (other.publicationYear != null)
				return false;
		} else if (!publicationYear.equals(other.publicationYear))
			return false;
		return true;
	}


}

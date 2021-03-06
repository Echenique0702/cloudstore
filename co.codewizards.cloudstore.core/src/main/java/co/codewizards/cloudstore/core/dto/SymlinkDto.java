package co.codewizards.cloudstore.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("serial")
@XmlRootElement
public class SymlinkDto extends RepoFileDto {

	private String target;

	public SymlinkDto() { }

	public String getTarget() {
		return target;
	}
	public void setTarget(final String target) {
		this.target = target;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties()
				+ ", target=" + target;
	}
}

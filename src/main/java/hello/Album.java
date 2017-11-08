package hello;

import javax.persistence.Entity;
import javax.persistence.Id;


@Entity
public class Album {

	@Id
	private String title;
	
	private String artist;
	private int releaseYear;
	private String genre;
	
	public Album() {
		
	}
	
	public Album(String title) {
		this.title = title;
	}
	
	public Album(String title, String artist, int releaseYear, String genre) {
		this.title = title;
		this.artist = artist;
		this.releaseYear = releaseYear;
		this.genre = genre;
	}
	
	public String toString() {
		return "Album: \n" + title + "\n" + artist + "\n" + releaseYear + "\n" + genre;
	}
	
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getReleaseYear() {
		return releaseYear;
	}
	public void setReleaseYear(int releaseYear) {
		this.releaseYear = releaseYear;
	}
	public String getGenre() {
		return genre;
	}
	public void setGenre(String genre) {
		this.genre = genre;
	}
	
}

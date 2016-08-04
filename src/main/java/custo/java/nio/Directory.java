package custo.java.nio;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Directory {
	/**
	 * This field is the directory's {@link Path}
     */
	private Path path;
	/**
	 * This is the {@link WatchService}. We only need to have one {@link WatchService} for all instances.
     */
	private static WatchService watchService;
	/**
	 * This {@link HashMap} represents all listened directories. It <b><u>NOT</u></b> contains the unlistened directories.
     */
	private static final HashMap<WatchKey, Directory> listenedDirectories = new HashMap<>();
	/**
	 * This {@link WatchKey} is a Key which is necessary in order to listen for a directory.
     */
	private WatchKey directoryKey;
	/**
	 * This {@link LinkedList} represents all event concerning the directory
     */
	private final LinkedList<DirectoryListener> directoryListeners = new LinkedList<>();

	/**
	 * Static init block which init the {@link WatchService} and running a new {@link Thread}, which receive the events.
     */
	static {
		try {
			watchService = FileSystems.getDefault().newWatchService();
			Directory.startListening();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see Files#createDirectory(Path, FileAttribute[])
	 * This method create the directory identified by the path param.
	 * @param path The path of the new directory
	 * @param options The options of the file attribute
	 * @throws IOException
     */
	public static Directory mkdirs(String path, FileAttribute<?>... options) throws IOException {
		Files.createDirectory(Paths.get(path), options);
		return new Directory(path);
	}

	/**
	 * This constructor must be call if the directory isn't created yet.
	 * @param path The path of the {@link Directory}, in {@link String} format
	 * @param toCreate boolean for creation
	 * @throws IOException
     */
	public Directory ( String path, boolean toCreate ) throws IOException {
	    this(Paths.get(path));
		if (toCreate) {
			Files.createDirectory(this.path);
		}
	}

	/**
	 * This constructor call the {@link Directory#Directory(Path)}.
	 * @param path The path which represent the {@link Directory}, in {@link String} format
	 * @throws IOException When the file pointed by the {@link Path} isn't a directory. Throw by {@link Directory#setPath(Path)}.
     */
	public Directory(String path) throws IOException {
		this(Paths.get(path));
	}

	/**
	 * This  constructor is the standard constructor. It instantiates a new {@link Directory} object.
	 * @param path The path which represent the {@link Directory}, in {@link Path} format.
	 * @throws IOException When the file pointed by the {@link Path} isn't a directory. Throw by {@link Directory#setPath(Path)}.
     */
	public Directory(Path path) throws IOException {
		this.setPath(path);
	}

	/**
	 * The getter for the {@link Directory#path}
	 * @return
     */
	public Path getPath() {
		return path;
	}

	/**
	 * The setter for the {@link Directory#path}
	 * @param path The path which represent the {@link Directory}, in {@link Path} format.
	 * @throws IOException When the file pointed by the {@link Path} isn't a directory.
     */
	public void setPath(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			this.path = path;
		} else {
			throw new IOException(path + " is not a directory");
		}
	}

	/**
	 * This method adds listener for current {@link Directory}. The listener must be an object which extend the {@link FunctionalInterface} {@link DirectoryListener}.
	 * @param listener An object which extends a {@link DirectoryListener}
     */
	public void addDirectoryListener(DirectoryListener listener) {
		if (!listenedDirectories.containsValue(this)) {
			try {
				this.directoryKey = this.path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
				listenedDirectories.put(directoryKey, this);
				System.out.println(directoryKey);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}

		this.directoryListeners.add(listener);
	}

	/**
	 * This method removes the directory listener represented by the param from the Listener's list. If no one left in the list, the {@link Directory} cease to be listen.
	 * @param listener The object which extends {@link DirectoryListener} to remove.
     */
	public void removeDirectoryListener(DirectoryListener listener) {
		this.directoryListeners.remove(listener);
		if (this.directoryListeners.isEmpty()) {
			listenedDirectories.remove(this);
			this.directoryKey.cancel();
		}
	}

	/**
	 * This method returns a {@link List} of {@link Path} which represents the content in the {@link Directory}.
	 * @return A list of Subfiles;
	 * @throws IOException When the {@link Files} object can't instantiate a new {@link DirectoryStream}
     */
	public List<Path> getSubPaths() throws IOException {
		ArrayList<Path> paths = new ArrayList<>();
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.path);
		directoryStream.forEach((path) -> paths.add(path));
		return paths;
	}

	/**
	 * This private method aim to launch a new {@link Thread} which is dedicated to listening incoming events.
	 * The {@link Thread} launch the {@link Directory#processDirectoryListener()} method in a {@link Runnable} context.
     */
	private static void startListening() {
		new Thread(() -> Directory.processDirectoryListener(), "Thread2ListenOnDirectoryChange").start();
	}

	/**
	 * @see WatchService
	 * @see WatchKey
	 * This method must be launch by {@link Directory#startListening()}. It loops constantly and force the current {@link Thread} to wait by {@link WatchService#take()}.
	 * When a directory notify the {@link WatchService} for a {@link WatchEvent}, it iterate in order over the {@link DirectoryListener} provided by {@link Directory#addDirectoryListener(DirectoryListener)},
	 * Then it reset the {@link WatchKey} which represent the {@link Directory} with {@link WatchKey#reset()} in order to tell the {@link WatchService} that all the events is consummed.
     */
	private static void processDirectoryListener() {
		for (;;) {
			try {
				WatchKey key = Directory.watchService.take();
				System.out.println(key);
				List<WatchEvent<?>> events = key.pollEvents();
				for (DirectoryListener listener : listenedDirectories.get(key).directoryListeners) {
					listener.onEvent(key, events);
				}
				boolean valid = key.reset();
				if (!valid) {
					System.out.println("Pas Valide");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Directory {
	private Path path;
	private static WatchService watchService;
	private static final HashMap<WatchKey, Directory> listenedDirectories = new HashMap<>();
	private WatchKey directoryKey;
	private final LinkedList<DirectoryListener> directoryListeners = new LinkedList<>();

	static {
		try {
			watchService = FileSystems.getDefault().newWatchService();
			Directory.startListening();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Directory(String path) throws IOException {
		this(Paths.get(path));
	}

	public Directory(Path path) throws IOException {
		this.setPath(path);
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			this.path = path;
		} else {
			throw new IOException(path + " is not a directory");
		}
	}

	public void addDirectoryListener(DirectoryListener listener) {
		if (!listenedDirectories.containsValue(this)) {
			try {
				this.directoryKey = this.path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
				listenedDirectories.put(directoryKey, this);
				System.out.println(directoryKey);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		this.directoryListeners.add(listener);
	}

	public void removeDirectoryListener(DirectoryListener listener) {
		this.directoryListeners.remove(listener);
		if (this.directoryListeners.isEmpty()) {
			listenedDirectories.remove(this);
			this.directoryKey.cancel();
		}
	}

	public List<Path> getSubFiles() throws IOException {
		ArrayList<Path> paths = new ArrayList<>();
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.path);
		directoryStream.forEach((path) -> {
			paths.add(path);
		});
		return paths;
	}

	private static void startListening() {
		new Thread(new Runnable() {
			public void run() {
				Directory.processDirectoryListener();
			}
		}).start();
	}

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

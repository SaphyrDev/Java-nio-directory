package custo.java.nio;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.List;

public interface DirectoryListener {
	public void onEvent(WatchKey key, List<WatchEvent<?>> events);
}
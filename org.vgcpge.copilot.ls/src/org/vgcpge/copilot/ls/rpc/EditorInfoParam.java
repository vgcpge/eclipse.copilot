package org.vgcpge.copilot.ls.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * 
 * 
 * <pre>
 * "method": "setEditorInfo",
    "params": {
        "editorInfo": {
            "name": "JetBrains-IC",
            "version": "231.8109.175"
        },
        "editorPluginInfo": {
            "name": "copilot-intellij",
            "version": "1.2.5.2507"
        },
        "editorConfiguration": {
            "showEditorCompletions": false,
            "enableAutoCompletions": true,
            "disabledLanguages": []
        }
    }
 * </pre>
 */
public class EditorInfoParam {
	public static final class EditorInfo {
		public final String name = "VGCPGE Eclipse Copilot";
		public final String version = "4.28";
	}
	public static final class EditorPluginInfo {
		public final String name = "VGCPGE Eclipse Copilot";
		public final String version = getVersion();

		// Credits:
		// https://huongdanjava.com/get-the-application-version-information-from-the-meta-inf-manifest-mf-file-inside-the-jar-file.html
		public static String getVersion() {
			try (InputStream stream = EditorInfoParam.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
				Manifest manifest = new Manifest(stream);
				Attributes attr = manifest.getMainAttributes();
				return Objects.requireNonNull(attr.getValue("Bundle-Version"));
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	public static final class NetworkProxy {
		@NonNull
		public final String host;
		public final int port;
		public final boolean rejectUnauthorized = true;
		public final String username, password;
		public NetworkProxy(String host, int port, String username, String password) {
			super();
			this.host = Objects.requireNonNull(host);
			this.port = port;
			this.username = username;
			this.password = password;
		}
		
	}
	public final EditorInfo editorInfo = new EditorInfo();
	public final EditorPluginInfo editorPluginInfo = new EditorPluginInfo();
	public final NetworkProxy networkProxy;

	public EditorInfoParam(Optional<NetworkProxy> networkProxy) {
		super();
		this.networkProxy = networkProxy.orElse(null);
	}
	
}

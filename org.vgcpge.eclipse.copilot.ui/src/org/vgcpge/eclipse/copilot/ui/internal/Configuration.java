package org.vgcpge.eclipse.copilot.ui.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.vgcpge.copilot.ls.ProxyConfiguration;

final class Configuration {
	private static final URI COPILOT_BACKEND_URI = URI
			.create("https://copilot-proxy.githubusercontent.com/v1/engines/copilot-codex/completions");

	public static Optional<ProxyConfiguration> getProxyConfiguration() throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(GithubCopilotProvider.class);
		try {
			start(bundle);
		} catch (BundleException e) {
			throw new IOException(e);
		}
		BundleContext context = bundle.getBundleContext();
		if (context == null) {
			return Optional.empty();
		}
		ServiceReference<IProxyService> reference = context.getServiceReference(IProxyService.class);
		if (reference == null) {
			return Optional.empty();
		}
		IProxyService proxyService = context.getService(reference);
		if (proxyService == null) {
			return Optional.empty();
		}
		try {
			IProxyData[] proxy = proxyService.select(COPILOT_BACKEND_URI);
			return Arrays.stream(proxy).findFirst().map(data -> new ProxyConfiguration(data.getHost(), data.getPort(),
					data.getUserId(), data.getPassword(), data.isRequiresAuthentication()));
		} finally {
			context.ungetService(reference);
		}
	}
	
	/** Copied from org.eclipse.core.internal.runtime.InternalPlatform.start()
	 */
	private static void start(Bundle bundle) throws BundleException {
		int originalState = bundle.getState();
		if ((originalState & Bundle.ACTIVE) != 0)
			return; // bundle is already active
		try {
			// attempt to activate the bundle
			bundle.start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			if ((originalState & Bundle.STARTING) != 0 && (bundle.getState() & Bundle.STARTING) != 0)
				// This can happen if the bundle was in the process of being activated on this
				// thread, just return
				return;
			throw e;
		}
	}
}

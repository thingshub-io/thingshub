package io.thingshub.ignite;

import static org.apache.ignite.internal.processors.cache.persistence.IgniteCacheDatabaseSharedManager.INTERNAL_DATA_REGION_NAMES;
import static org.apache.ignite.internal.util.IgniteUtils.MB;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.plugin.IgniteLogInfoProviderImpl;
import org.apache.ignite.internal.processors.cache.persistence.DataRegion;
import org.apache.ignite.internal.processors.cache.persistence.IgniteCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.port.GridPortRecord;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.SB;
import org.apache.ignite.internal.util.typedef.internal.U;

public class CustomIgniteLogInfoProvider extends IgniteLogInfoProviderImpl {

	private final DecimalFormat decimalFormat = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.CHINESE));

	@Override
	public void ackKernalStarted(IgniteLogger log, Ignite ignite) {
		IgniteEx igEx = (IgniteEx) ignite;

		GridKernalContext ctx = igEx.context();

		ctx.performance().add("Disable assertions (remove '-ea' from JVM options)", !U.assertionsEnabled());
		ctx.performance().logSuggestions(log, ignite.name());

		ClusterNode locNode = igEx.localNode();
		if (log.isQuiet()) {
			U.quiet(false, "");

			U.quiet(false, "Ignite node started OK (id=" + U.id8(locNode.id()) + (F.isEmpty(ignite.name()) ? "" : ", instance name=" + ignite.name()) + ')');
		}

		if (log.isInfoEnabled()) {
			SB sb = new SB();

			for (GridPortRecord rec : ctx.ports().records()) {
				sb.a(rec.protocol()).a(":").a(rec.port()).a(" ");
			}
			sb.setLength(sb.length() - 1);

			String str = "Ignite Node Info [" //
					+ "CPU(s): " + locNode.metrics().getTotalCpus() + ", " //
					+ "Heap: " + U.heapSize(locNode, 2) + "GB" + ", " //
					+ "VM name: " + ((IgniteKernal) ignite).vmName() //
					+ (ignite.name() == null ? "" : ", instance name: " + ignite.name()) + ", " //
					+ "node id: " + locNode.id().toString().toUpperCase() + ", " //
					+ "node addresses: " + U.addressesAsString(locNode) + "," //
					+ "local ports: " + sb + "]";

			log.info(str);
		}
	}

	@Override
	public void ackNodeDataStorageMetrics(IgniteLogger log, Ignite ignite) {
		GridKernalContext ctx = ((IgniteEx) ignite).context();
		IgniteCacheDatabaseSharedManager db = ctx.cache().context().database();
		if (F.isEmpty(db.dataRegions())) {
			return;
		}

		SB dataRegionsInfo = new SB();
		dataRegionsInfo.a("Data Storage Metrics: ").nl();
		int i = db.dataRegions().size();
		for (DataRegion region : db.dataRegions()) {
			DataRegionConfiguration regCfg = region.config();

			long pagesCnt = region.pageMemory().loadedPages();

			long offHeapUsed = region.pageMemory().systemPageSize() * pagesCnt;
			long offHeapInit = regCfg.getInitialSize();
			long offHeapMax = regCfg.getMaxSize();
			long offHeapComm = region.metrics().getOffHeapSize();

			long offHeapUsedInMBytes = offHeapUsed / MB;
			long offHeapMaxInMBytes = offHeapMax / MB;
			long offHeapCommInMBytes = offHeapComm / MB;
			long offHeapInitInMBytes = offHeapInit / MB;

			double freeOffHeapPct = offHeapMax > 0 ? ((double) ((offHeapMax - offHeapUsed) * 100)) / offHeapMax : -1;

			String type = "user";

			try {
				if (region == db.dataRegion(null))
					type = "default";
				else if (INTERNAL_DATA_REGION_NAMES.contains(regCfg.getName()))
					type = "internal";
			} catch (IgniteCheckedException ice) {
				ice.printStackTrace();
			}

			dataRegionsInfo.a("  ").a(regCfg.getName()).a(" region: [") //
					.a("type=").a(type).a(", ") //
					.a("persistence=").a(regCfg.isPersistenceEnabled()).a(", ")//
					.a("lazyAlloc=").a(regCfg.isLazyMemoryAllocation()).a(", ")//
					.a("initCfg=").a(decimalFormat.format(offHeapInitInMBytes)).a("MB, ") //
					.a("maxCfg=").a(decimalFormat.format(offHeapMaxInMBytes)).a("MB, ") //
					.a("usedRam=").a(decimalFormat.format(offHeapUsedInMBytes)).a("MB, ") //
					.a("freeRam=").a(decimalFormat.format(freeOffHeapPct)).a("%, ") //
					.a("allocRam=").a(decimalFormat.format(offHeapCommInMBytes)).a("MB");
			if (regCfg.isPersistenceEnabled()) {
				dataRegionsInfo.a(", ") //
						.a("allocTotal=").a(decimalFormat.format(region.metrics().getTotalAllocatedSize() / MB)).a("MB");
			}

			if (i > 1) {
				dataRegionsInfo.a(']').nl();
				i--;
			} else {
				dataRegionsInfo.a(']');
			}
		}

		log.info(dataRegionsInfo.toString());
	}

	@Override
	public void ackKernalStopped(IgniteLogger log, Ignite ignite, boolean err) {
		String igniteInstanceName = ignite.name();

		if (!err) {
			log.info("Ignite node [" + igniteInstanceName + "] stopped OK");
		} else {
			log.error("Ignite node [" + igniteInstanceName + "] stopped with ERRORS. See log above for detailed error message.");
		}
	}

}

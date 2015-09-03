// package com.goodformentertainment.canary.sky;
//
// import net.canarymod.logger.Logman;
// import org.easymock.EasyMockSupport;
// import org.junit.Before;
// import org.junit.Test;
//
// import com.goodformentertainment.canary.sky.BlockScoreValue;
// import com.goodformentertainment.canary.sky.SkylandsPlugin;
// import com.goodformentertainment.canary.sky.SkylandsConfig;
//
// import java.util.Collection;
// import java.util.HashSet;
//
// import static org.easymock.EasyMock.*;
//
// public class BlockScoreValueTest extends EasyMockSupport {
// private SkylandsConfig mockConfig;
// private Logman mockLog;
// private BlockScoreValue blockScoreValues;
//
// @Before
// public void init() {
// mockConfig = createMock(SkylandsConfig.class);
// mockLog = createMock(Logman.class);
// SkylandsPlugin.LOG = mockLog;
// }
//
// @Test
// public void test() {
// final Collection<String> ignoredRemoves = new HashSet<String>();
// ignoredRemoves.add("cobblestone");
// ignoredRemoves.add("tree");
//
// expect(mockConfig.getIgnoredBlockRemoves()).andReturn(ignoredRemoves);
// expect(mockConfig.getBlockTypeValue(isA(String.class))).andReturn(2).anyTimes();
// expect(mockConfig.getBlockVariantValueMultiplier(isA(String.class))).andReturn(2).anyTimes();
// expect(mockConfig.getBlockColorValueMultiplier(isA(String.class))).andReturn(2).anyTimes();
// mockLog.debug(isA(String.class));
// expectLastCall().anyTimes();
//
// replayAll();
//
// blockScoreValues = new BlockScoreValue(mockConfig);
//
// System.out.println("Place Values size:\n" + blockScoreValues.getPlaceValues().size());
// System.out.println("Remove Values size:\n" + blockScoreValues.getRemoveValues().size());
//
// verifyAll();
// }
// }

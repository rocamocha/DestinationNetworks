package net.cyootlabs.rocamocha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Loader-agnostic store of teleporter links.
 * <p>
 * Each link keeps its owner (permanent), a {@code global} flag (usable by everyone with no
 * permission) and a set of members (players who linked someone else's block into their own chain).
 * A player's chain for a color is: all global blocks + their own blocks + blocks they are a member
 * of. Ownership never changes.
 */
public class TeleporterData {
	public static final int MAX_RECENT_DESTINATIONS = 10;

	public static final class Link {
		public final int[] pos = new int[3];
		public UUID owner;
		public String ownerName = "";
		public String name = "";
		public boolean global;
		public final Set<UUID> members = new HashSet<>();

		public boolean occupied() {
			return this.pos[0] != 0 || this.pos[1] != 0 || this.pos[2] != 0;
		}
	}

	public static final class RecentDestination {
		public final TeleporterColor color;
		public final int[] pos;

		private RecentDestination(TeleporterColor color, int[] pos) {
			this.color = color;
			this.pos = pos.clone();
		}

		public int x() {
			return this.pos[0];
		}

		public int y() {
			return this.pos[1];
		}

		public int z() {
			return this.pos[2];
		}
	}

	private final Map<TeleporterColor, List<Link>> links = new EnumMap<>(TeleporterColor.class);
	private final Map<UUID, Set<UUID>> invites = new HashMap<>();
	private final Map<UUID, List<RecentDestination>> recentDestinations = new HashMap<>();

	private List<Link> slots(TeleporterColor color) {
		return this.links.computeIfAbsent(color, key -> new ArrayList<>());
	}

	public int slotCount(TeleporterColor color) {
		return this.slots(color).size();
	}

	public int[] getSlot(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() ? list.get(index).pos : new int[3];
	}

	public UUID getOwner(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() ? list.get(index).owner : null;
	}

	public String getName(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() ? list.get(index).name : "";
	}

	public String getOwnerName(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() ? list.get(index).ownerName : "";
	}

	public boolean isGlobal(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() && list.get(index).global;
	}

	public boolean isSlotOccupied(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() && list.get(index).occupied();
	}

	public void setSlot(TeleporterColor color, int index, double x, double y, double z, boolean global) {
		List<Link> list = this.slots(color);
		while (list.size() <= index) {
			list.add(new Link());
		}
		Link link = list.get(index);
		link.pos[0] = (int) x;
		link.pos[1] = (int) y;
		link.pos[2] = (int) z;
		link.owner = null;
		link.global = global;
		link.members.clear();
	}

	public void setOwner(TeleporterColor color, double x, double y, double z, UUID owner) {
		this.setOwner(color, x, y, z, owner, "");
	}

	public void setOwner(TeleporterColor color, double x, double y, double z, UUID owner, String ownerName) {
		int index = this.findSlot(color, x, y, z);
		net.cyootlabs.rocamocha.Constants.LOG.info("JAKEDBG data.setOwner color={} at {} {} {} index={} owner={}",
				color.getId(), x, y, z, index, owner);
		if (index >= 0) {
			Link link = this.slots(color).get(index);
			link.owner = owner;
			if (ownerName != null && !ownerName.isBlank())
				link.ownerName = ownerName;
		}
	}

	public void setName(TeleporterColor color, int index, String name) {
		List<Link> list = this.slots(color);
		if (index >= 0 && index < list.size())
			list.get(index).name = name == null ? "" : name.trim();
	}

	public void setGlobal(TeleporterColor color, int index, boolean global) {
		List<Link> list = this.slots(color);
		if (index >= 0 && index < list.size()) {
			Link link = list.get(index);
			link.global = global;
			if (!global && link.owner != null) {
				// Making a block personal immediately drops it from everyone else's chain.
				link.members.clear();
			}
		}
	}

	public void clearSlot(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		if (index >= 0 && index < list.size()) {
			Link link = list.get(index);
			link.pos[0] = 0;
			link.pos[1] = 0;
			link.pos[2] = 0;
			link.owner = null;
			link.global = false;
			link.members.clear();
		}
	}

	public int findSlot(TeleporterColor color, double x, double y, double z) {
		List<Link> list = this.slots(color);
		for (int i = 0; i < list.size(); i++) {
			int[] pos = list.get(i).pos;
			if (pos[0] == x && pos[1] == y && pos[2] == z)
				return i;
		}
		return -1;
	}

	public int findEmptySlot(TeleporterColor color) {
		List<Link> list = this.slots(color);
		for (int i = 0; i < list.size(); i++) {
			if (!list.get(i).occupied())
				return i;
		}
		return list.size();
	}

	/** Identifies a specific link by reference, since duplicate slots can share the same position. */
	public int indexOf(TeleporterColor color, Link link) {
		return this.slots(color).indexOf(link);
	}

	/** Returns another occupied slot sharing the given slot's position, if a legacy save left a duplicate. */
	public int findDuplicateSlot(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		if (index < 0 || index >= list.size())
			return -1;
		Link link = list.get(index);
		if (!link.occupied())
			return -1;
		for (int i = 0; i < list.size(); i++) {
			if (i == index)
				continue;
			Link other = list.get(i);
			if (other.occupied() && other.pos[0] == link.pos[0] && other.pos[1] == link.pos[1] && other.pos[2] == link.pos[2])
				return i;
		}
		return -1;
	}

	public int countOwned(TeleporterColor color, UUID owner) {
		int count = 0;
		for (Link link : this.slots(color)) {
			if (link.occupied() && owner.equals(link.owner))
				count++;
		}
		return count;
	}

	public void addMember(TeleporterColor color, int index, UUID player) {
		List<Link> list = this.slots(color);
		if (index >= 0 && index < list.size()) {
			list.get(index).members.add(player);
		}
	}

	public boolean removeMember(TeleporterColor color, int index, UUID player) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() && list.get(index).members.remove(player);
	}

	public boolean isMember(TeleporterColor color, int index, UUID player) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() && list.get(index).members.contains(player);
	}

	public Set<UUID> membersOf(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		return index >= 0 && index < list.size() ? Collections.unmodifiableSet(list.get(index).members) : Collections.emptySet();
	}

	public void clearMembers(TeleporterColor color, int index) {
		List<Link> list = this.slots(color);
		if (index >= 0 && index < list.size()) {
			list.get(index).members.clear();
		}
	}

	public boolean isOwner(TeleporterColor color, int index, UUID player) {
		UUID owner = this.getOwner(color, index);
		return owner == null || owner.equals(player);
	}

	public boolean canUseBlock(TeleporterColor color, int index, UUID player) {
		List<Link> list = this.slots(color);
		if (index < 0 || index >= list.size())
			return false;
		Link link = list.get(index);
		if (link.global || link.owner == null || link.owner.equals(player) || link.members.contains(player))
			return true;
		return this.isInvited(link.owner, player);
	}

	/**
	 * Legacy links have no owner because they were placed before ownership was introduced. They
	 * remain breakable by anyone; only links with an owner are protected.
	 */
	public boolean canBreakBlock(TeleporterColor color, int index, UUID player) {
		List<Link> list = this.slots(color);
		if (index < 0 || index >= list.size())
			return true;
		UUID owner = list.get(index).owner;
		return owner == null || owner.equals(player);
	}

	/**
	 * @return positions in the given player's chain for the color, in slot order.
	 */
	public List<int[]> chainFor(UUID player, TeleporterColor color) {
		List<int[]> result = new ArrayList<>();
		for (Link link : this.slots(color)) {
			if (!link.occupied())
				continue;
			if (link.global || link.owner == null || (player != null && (player.equals(link.owner) || link.members.contains(player))))
				result.add(link.pos.clone());
		}
		return result;
	}

	/** Returns accessible links without exposing the mutable backing lists. */
	public List<Link> accessibleLinks(UUID player, TeleporterColor color) {
		List<Link> result = new ArrayList<>();
		for (Link link : this.slots(color)) {
			if (link.occupied() && (link.global || link.owner == null || (player != null && (player.equals(link.owner) || link.members.contains(player)))))
				result.add(link);
		}
		return result;
	}

	public List<Link> accessibleLinks(UUID player) {
		List<Link> result = new ArrayList<>();
		for (TeleporterColor color : TeleporterColor.values())
			result.addAll(this.accessibleLinks(player, color));
		return result;
	}

	public void recordRecentDestination(UUID player, TeleporterColor color, int x, int y, int z) {
		if (player == null || color == null)
			return;
		List<RecentDestination> recent = this.recentDestinations.computeIfAbsent(player, key -> new ArrayList<>());
		recent.removeIf(entry -> entry.color == color && entry.x() == x && entry.y() == y && entry.z() == z);
		recent.add(0, new RecentDestination(color, new int[] { x, y, z }));
		if (recent.size() > MAX_RECENT_DESTINATIONS)
			recent.subList(MAX_RECENT_DESTINATIONS, recent.size()).clear();
	}

	public List<RecentDestination> recentDestinations(UUID player) {
		return player == null ? List.of() : List.copyOf(this.recentDestinations.getOrDefault(player, List.of()));
	}

	public int indexInChain(List<int[]> chain, double x, double y, double z) {
		for (int i = 0; i < chain.size(); i++) {
			int[] pos = chain.get(i);
			if (pos[0] == x && pos[1] == y && pos[2] == z)
				return i;
		}
		return -1;
	}

	public void invite(UUID owner, UUID target) {
		this.invites.computeIfAbsent(owner, key -> new HashSet<>()).add(target);
	}

	public boolean removeInvite(UUID owner, UUID target) {
		Set<UUID> allowed = this.invites.get(owner);
		return allowed != null && allowed.remove(target);
	}

	public boolean isInvited(UUID owner, UUID target) {
		Set<UUID> allowed = this.invites.get(owner);
		return allowed != null && allowed.contains(target);
	}

	public Set<UUID> invitesOf(UUID owner) {
		return this.invites.getOrDefault(owner, Collections.emptySet());
	}

	/**
	 * Removes the given player from every chain link owned by {@code owner}.
	 */
	public void purgeMember(UUID owner, UUID target) {
		for (List<Link> list : this.links.values()) {
			for (Link link : list) {
				if (owner.equals(link.owner))
					link.members.remove(target);
			}
		}
	}

	public void read(CompoundTag nbt) {
		for (TeleporterColor color : TeleporterColor.values()) {
			List<Link> list = this.slots(color);
			list.clear();
			for (int i = 0; nbt.contains(color.getId() + "x" + (i + 1)); i++) {
				Link link = new Link();
				link.pos[0] = (int) nbt.getDouble(color.getId() + "x" + (i + 1));
				link.pos[1] = (int) nbt.getDouble(color.getId() + "y" + (i + 1));
				link.pos[2] = (int) nbt.getDouble(color.getId() + "z" + (i + 1));

				String ownerKey = color.getId() + "o" + (i + 1);
				if (nbt.contains(ownerKey)) {
					try {
						link.owner = UUID.fromString(nbt.getString(ownerKey));
					} catch (IllegalArgumentException ignored) {
					}
				}
				String ownerNameKey = color.getId() + "n" + (i + 1);
				if (nbt.contains(ownerNameKey))
					link.ownerName = nbt.getString(ownerNameKey);
				String nameKey = color.getId() + "l" + (i + 1);
				if (nbt.contains(nameKey))
					link.name = nbt.getString(nameKey);
				// Old saves have no global flag: legacy blocks are shared by default.
				String globalKey = color.getId() + "g" + (i + 1);
				link.global = !nbt.contains(globalKey) || nbt.getBoolean(globalKey);

				String membersKey = color.getId() + "k" + (i + 1);
				if (nbt.contains(membersKey, Tag.TAG_LIST)) {
					ListTag members = nbt.getList(membersKey, Tag.TAG_STRING);
					for (int m = 0; m < members.size(); m++) {
						try {
							link.members.add(UUID.fromString(members.getString(m)));
						} catch (IllegalArgumentException ignored) {
						}
					}
				}
				list.add(link);
			}
		}

		this.invites.clear();
		this.recentDestinations.clear();
		if (nbt.contains("permissions", Tag.TAG_COMPOUND)) {
			CompoundTag permissions = nbt.getCompound("permissions");
			for (String key : permissions.getAllKeys()) {
				UUID owner;
				try {
					owner = UUID.fromString(key);
				} catch (IllegalArgumentException e) {
					continue;
				}
				ListTag list = permissions.getList(key, Tag.TAG_STRING);
				Set<UUID> allowed = new HashSet<>();
				for (int i = 0; i < list.size(); i++) {
					try {
						allowed.add(UUID.fromString(list.getString(i)));
					} catch (IllegalArgumentException ignored) {
					}
				}
				if (!allowed.isEmpty())
					this.invites.put(owner, allowed);
			}
		}
		if (nbt.contains("recent_destinations", Tag.TAG_COMPOUND)) {
			CompoundTag recentTag = nbt.getCompound("recent_destinations");
			for (String playerKey : recentTag.getAllKeys()) {
				try {
					UUID player = UUID.fromString(playerKey);
					ListTag entries = recentTag.getList(playerKey, Tag.TAG_COMPOUND);
					List<RecentDestination> recent = this.recentDestinations.computeIfAbsent(player, key -> new ArrayList<>());
					for (int i = 0; i < Math.min(entries.size(), MAX_RECENT_DESTINATIONS); i++) {
						CompoundTag entry = entries.getCompound(i);
						TeleporterColor color = TeleporterColor.byId(entry.getString("color"));
						if (color != null)
							recent.add(new RecentDestination(color, new int[] { entry.getInt("x"), entry.getInt("y"), entry.getInt("z") }));
					}
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
	}

	public CompoundTag write(CompoundTag nbt) {
		for (TeleporterColor color : TeleporterColor.values()) {
			List<Link> list = this.slots(color);
			for (int i = 0; i < list.size(); i++) {
				Link link = list.get(i);
				nbt.putDouble(color.getId() + "x" + (i + 1), link.pos[0]);
				nbt.putDouble(color.getId() + "y" + (i + 1), link.pos[1]);
				nbt.putDouble(color.getId() + "z" + (i + 1), link.pos[2]);
				if (link.owner != null)
					nbt.putString(color.getId() + "o" + (i + 1), link.owner.toString());
				if (!link.ownerName.isBlank())
					nbt.putString(color.getId() + "n" + (i + 1), link.ownerName);
				if (!link.name.isBlank())
					nbt.putString(color.getId() + "l" + (i + 1), link.name);
				nbt.putBoolean(color.getId() + "g" + (i + 1), link.global);
				if (!link.members.isEmpty()) {
					ListTag members = new ListTag();
					for (UUID uuid : link.members) {
						members.add(StringTag.valueOf(uuid.toString()));
					}
					nbt.put(color.getId() + "k" + (i + 1), members);
				}
			}
		}

		CompoundTag permissions = new CompoundTag();
		this.invites.forEach((owner, allowed) -> {
			ListTag list = new ListTag();
			for (UUID uuid : allowed) {
				list.add(StringTag.valueOf(uuid.toString()));
			}
			permissions.put(owner.toString(), list);
		});
		nbt.put("permissions", permissions);
		CompoundTag recentTag = new CompoundTag();
		this.recentDestinations.forEach((player, entries) -> {
			ListTag list = new ListTag();
			for (RecentDestination entry : entries) {
				CompoundTag value = new CompoundTag();
				value.putString("color", entry.color.getId());
				value.putInt("x", entry.x());
				value.putInt("y", entry.y());
				value.putInt("z", entry.z());
				list.add(value);
			}
			recentTag.put(player.toString(), list);
		});
		nbt.put("recent_destinations", recentTag);
		return nbt;
	}
}

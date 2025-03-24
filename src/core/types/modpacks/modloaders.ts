import { Art } from '@/core/types/appTypes.ts';

export type ModLoadersResponse = {
  id: string;
  name: string;
  art: Art[];
	loaders: ModLoader[];
	total: number;
	refreshed: number;
}

export type ModLoader = {
	id: number;
	version: string;
	game: string;
	gameVersion: string;
	type: string;
}

export type ModLoaderWithPackId = ModLoader & {
  packId: string;
}
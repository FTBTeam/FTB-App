export type ModLoadersResponse = {
	loaders: ModLoader[];
	total: number;
	refreshed: number;
}

export type ModLoader = {
	id: number;
	pack: number;
	version: string;
	game: string;
	gameVersion: string;
	type: string;
}
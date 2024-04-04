<template>
 <div class="favorites page-spacing">
   <div class="flex justify-between items-center mb-6">
     <div>
       <h2 class="text-lg font-bold">Favorites</h2>
       <p class="text-muted">Here you can see and manage your favourite modpacks.</p>
     </div>
     <div class="buttons" v-if="favourites.length">
       <ui-button type="danger" icon="trash" size="small" @click="removeAll">Remove all</ui-button>
     </div>
   </div>
   
   <div class="cards" v-if="favourites.length">
     <div class="card-wrapper" v-for="favourite in favourites">
       <pack-preview :key="favourite.packId" :packId="favourite.packId" :provider="favourite.provider" />
     </div>
   </div>
   
   <div class="bg-opacity-25 bg-black rounded p-4 py-16 text-center" v-else>
     <font-awesome-icon icon="star" class="text-6xl text-yellow-500 mb-8" />
     <p class="text-muted font-bold mb-2">No favorites yet</p>
     <p class="text-muted">You can add modpacks to your favorites by clicking the star icon on the modpack card.</p>
     
     <div class="buttons mt-8">
       <router-link :to="{name: RouterNames.ROOT_BROWSE_PACKS}">
         <ui-button icon="search" type="primary">Browse modpacks</ui-button>
       </router-link>
     </div>
   </div>
 </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {UserFavorite} from '@/core/state/misc/userFavouritesState';
import PackPreview from '@/components/core/modpack/PackPreview.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {dialogsController} from '@/core/controllers/dialogsController';
import {RouterNames} from '@/router';

@Component({
  components: {UiButton, PackPreview}
})
export default class Favorites extends Vue {
  @Getter('favourites', ns("v2/userFavourites")) favourites!: UserFavorite[];
  
  @Action('removeFavourite', { namespace: 'v2/userFavourites' }) removeFavourite!: (favourite: UserFavorite) => void;
  @Action('removeAllFavourites', { namespace: 'v2/userFavourites' }) removeAllFavourites!: () => void;
  
  RouterNames = RouterNames;
  
  async removeAll() {
    const res = await dialogsController.createConfirmationDialog("Are you sure you want to remove all favorites?", "This action cannot be undone.");
    if (res) {
      this.removeAllFavourites();
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
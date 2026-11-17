import {JavaFetch} from "@/core/javaFetch.ts";
import {constants} from "@/core/constants.ts";
import {BlogPost, Pagination} from "@/core/types/external/metaApi.types.ts";
import {createLogger} from "@/core/logger.ts";

class BlogController {
  private logger = createLogger("BlogController.ts");
  
  async getPosts(page: number = 1) {
    try {
      const newsReq = await JavaFetch
        .create(`${constants.metaApi}/blog/posts?page=${page}`)
        .execute();

      if (!newsReq) {
        return null;
      }

      return newsReq.json<{posts: BlogPost[], pagination: Pagination}>();
    } catch (e) {
      this.logger.error("Failed to load news", e);
      return null;
    }
  }
}

export const blogController = new BlogController();
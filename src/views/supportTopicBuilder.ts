import { IconDefinition } from '@fortawesome/free-brands-svg-icons';

type Topic = {
  name: string;
  desc: string;
  icon?: IconDefinition;
  customIcon?: string;
  link: string;
}

class TopicBuilder {
  private readonly name: string;
  private readonly desc: string;
  
  private _icon?: IconDefinition;
  private _customIcon?: string;
  private _goEndpoint?: string;
  private _url?: string;
  
  constructor(name: string, desc: string) {
    this.name = name;
    this.desc = desc;
  }
  
  icon(icon: IconDefinition): TopicBuilder {
    this._icon = icon;
    this._customIcon = undefined;
    return this;
  }
  
  customIcon(customIcon: string): TopicBuilder {
    this._customIcon = customIcon;
    this._icon = undefined;
    return this;
  }
  
  goEndpoint(goEndpoint: string): TopicBuilder {
    this._goEndpoint = goEndpoint;
    this._url = undefined;
    return this;
  }
  
  url(url: string): TopicBuilder {
    this._url = url;
    this._goEndpoint = undefined;
    return this;
  }
  
  build(): Topic {
    return {
      name: this.name,
      desc: this.desc,
      icon: this._icon,
      customIcon: this._customIcon,
      link: this._goEndpoint ? `https://go.ftb.team/${this._goEndpoint}` : this._url ? this._url : '',
    };
  }
}

export function topicBuilder(name: string, desc: string): TopicBuilder {
  return new TopicBuilder(name, desc);
}
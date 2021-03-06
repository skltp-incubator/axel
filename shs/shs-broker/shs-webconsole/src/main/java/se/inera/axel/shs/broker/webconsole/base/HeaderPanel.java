/**
 * Copyright (C) 2013 Inera AB (http://www.inera.se)
 *
 * This file is part of Inera Axel (http://code.google.com/p/inera-axel).
 *
 * Inera Axel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Inera Axel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package se.inera.axel.shs.broker.webconsole.base;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import se.inera.axel.shs.broker.routing.ShsRouter;
import se.inera.axel.shs.broker.webconsole.directory.ActorPage;

import javax.inject.Inject;
import javax.inject.Named;

public class HeaderPanel extends Panel {

	@Inject
    @Named("shsRouter")
    @SpringBean(name = "shsRouter")
	ShsRouter shsRouter;

	public HeaderPanel(final String id) {
		super(id);
		String orgNumber = shsRouter.getOrgId();
		PageParameters params = new PageParameters();
		params.add("orgNumber", orgNumber);
		Link<Void> link = new BookmarkablePageLink<>("orgNumber.link",
				ActorPage.class, params);
		link.add(new Label("orgNumber", orgNumber));
		add(link);
	}

	private static final long serialVersionUID = 1L;

}

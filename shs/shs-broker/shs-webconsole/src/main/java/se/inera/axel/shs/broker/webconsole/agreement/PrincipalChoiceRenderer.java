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
package se.inera.axel.shs.broker.webconsole.agreement;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

import se.inera.axel.shs.xml.agreement.Principal;

public class PrincipalChoiceRenderer implements IChoiceRenderer<Principal> {

	private static final long serialVersionUID = 1L;

	@Override
	public Object getDisplayValue(Principal principal) {
		String displayValue = null;
		if (principal != null) {
			displayValue = principal.getCommonName();
		}
		return displayValue;
	}

	@Override
	public String getIdValue(Principal principal, int index) {
		String idValue = null;
		if (principal != null) {
			idValue = principal.getValue();
		}
		return idValue;
	}

}
